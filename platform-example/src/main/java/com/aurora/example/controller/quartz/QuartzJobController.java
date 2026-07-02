package com.aurora.example.controller.quartz;

import com.aurora.starter.quartz.domain.QuartzJob;
import com.aurora.starter.quartz.service.IQuartzJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quartz 定时任务 Controller —— 演示业务方接入 quartz-spring-boot-starter.
 * <p>
 * 只引入 starter + 写 Controller + 写任务 Bean 三步即可,无需自写 Entity/Mapper/Service/JobBootstrap.
 */
@RestController
@RequestMapping("/api/quartz-job")
@RequiredArgsConstructor
public class QuartzJobController {

    private final IQuartzJobService quartzJobService;

    // ─────────────── 查询 ───────────────

    @GetMapping("/list")
    public List<QuartzJob> list() {
        return quartzJobService.list();
    }

    @GetMapping("/{jobId}")
    public QuartzJob getById(@PathVariable Long jobId) {
        return quartzJobService.getById(jobId);
    }

    // ─────────────── 增删改 ───────────────

    @PostMapping
    public String create(@RequestBody QuartzJob job) {
        try {
            quartzJobService.createJob(job);
            return "ok";
        } catch (Exception e) {
            return "创建失败: " + e.getMessage();
        }
    }

    @PutMapping
    public String update(@RequestBody QuartzJob job) {
        try {
            quartzJobService.updateJob(job);
            return "ok";
        } catch (Exception e) {
            return "更新失败: " + e.getMessage();
        }
    }

    @DeleteMapping("/{jobId}")
    public String delete(@PathVariable Long jobId,
                         @RequestParam(defaultValue = "DEFAULT") String group) {
        try {
            quartzJobService.deleteJob(jobId, group);
            return "ok";
        } catch (Exception e) {
            return "删除失败: " + e.getMessage();
        }
    }

    // ─────────────── 控制 ───────────────

    @PostMapping("/{jobId}/pause")
    public String pause(@PathVariable Long jobId,
                        @RequestParam(defaultValue = "DEFAULT") String group) {
        try {
            quartzJobService.pauseJob(jobId, group);
            return "ok";
        } catch (Exception e) {
            return "暂停失败: " + e.getMessage();
        }
    }

    @PostMapping("/{jobId}/resume")
    public String resume(@PathVariable Long jobId,
                         @RequestParam(defaultValue = "DEFAULT") String group) {
        try {
            quartzJobService.resumeJob(jobId, group);
            return "ok";
        } catch (Exception e) {
            return "恢复失败: " + e.getMessage();
        }
    }

    @PostMapping("/{jobId}/run")
    public String runNow(@PathVariable Long jobId,
                         @RequestParam(defaultValue = "DEFAULT") String group) {
        try {
            quartzJobService.triggerNow(jobId, group);
            return "ok,任务已触发";
        } catch (Exception e) {
            return "触发失败: " + e.getMessage();
        }
    }
}
