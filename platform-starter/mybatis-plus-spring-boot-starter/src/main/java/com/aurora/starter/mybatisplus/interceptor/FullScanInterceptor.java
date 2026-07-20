package com.aurora.starter.mybatisplus.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 全表扫描拦截器.
 *
 * @author Luo
 * @date 2023-06-09 10:09
 */
@Slf4j
public class FullScanInterceptor extends JsqlParserSupport implements InnerInterceptor {

    /**
     * 禁止查询的表（HashSet，O(1) 查询）.
     */
    private final Set<String> tables;

    public FullScanInterceptor(List<String> tables) {
        this.tables = tables == null ? Collections.emptySet() : new HashSet<>(tables);
    }

    @Override
    public void beforeQuery(final Executor executor, final MappedStatement ms, final Object parameter, final RowBounds rowBounds, final ResultHandler resultHandler, final BoundSql boundSql) throws SQLException {
        String methodName = ms.getId();
        if (!methodName.contains(".selectList")) {
            return;
        }
        if (StringUtils.containsIgnoreCase(boundSql.getSql(), "limit")
                || StringUtils.containsIgnoreCase(boundSql.getSql(), "count(0)")) {
            return;
        }
        if (InterceptorIgnoreHelper.willIgnoreBlockAttack(ms.getId())) {
            return;
        }
        parserMulti(boundSql.getSql(), null);
    }

    @Override
    protected void processSelect(final Select select, final int index, final String sql, final Object obj) {
        if (null == select) {
            return;
        }
        PlainSelect plainSelect = select.getPlainSelect();
        if (plainSelect == null) {
            return;
        }
        Expression where = plainSelect.getWhere();
        FromItem fromItem = plainSelect.getFromItem();
        if (!(fromItem instanceof Table table)) {
            return;
        }
        if (fullMatch(where, getTableLogicField(table.getName()))) {
            if (tables.contains(table.getName())) {
                log.error("查询表数据时未带任何条件，禁止查询[{}] \n {}",
                        table.getName(), ExceptionUtil.stacktraceToString(new Exception(), 5000));
                throw new BadSqlGrammarException("无参数全表扫描", sql, new SQLException("查询列表数据失败"));
            }
        }
    }

    /**
     * 匹配是否带条件.
     */
    private boolean fullMatch(Expression where, String logicField) {
        if (where == null) {
            return true;
        }
        if (StringUtils.isNotBlank(logicField)) {
            if (where instanceof BinaryExpression binaryExpression) {
                if (StringUtils.equals(binaryExpression.getLeftExpression().toString(), logicField)
                        || StringUtils.equals(binaryExpression.getRightExpression().toString(), logicField)) {
                    return true;
                }
            }
            if (where instanceof IsNullExpression isNullExpression) {
                if (StringUtils.equals(isNullExpression.getLeftExpression().toString(), logicField)) {
                    return true;
                }
            }
        }

        if (where instanceof EqualsTo equalsTo) {
            return StringUtils.equals(equalsTo.getLeftExpression().toString(), equalsTo.getRightExpression().toString());
        }
        if (where instanceof NotEqualsTo notEqualsTo) {
            return !StringUtils.equals(notEqualsTo.getLeftExpression().toString(), notEqualsTo.getRightExpression().toString());
        }
        if (where instanceof OrExpression orExpression) {
            return fullMatch(orExpression.getLeftExpression(), logicField)
                    || fullMatch(orExpression.getRightExpression(), logicField);
        }
        if (where instanceof AndExpression andExpression) {
            return fullMatch(andExpression.getLeftExpression(), logicField)
                    && fullMatch(andExpression.getRightExpression(), logicField);
        }
        return false;
    }

    /**
     * 获取表名中的逻辑删除字段.
     */
    private String getTableLogicField(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return StringPool.EMPTY;
        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(tableName);
        if (tableInfo == null || !tableInfo.isWithLogicDelete() || tableInfo.getLogicDeleteFieldInfo() == null) {
            return StringPool.EMPTY;
        }
        return tableInfo.getLogicDeleteFieldInfo().getColumn();
    }

}