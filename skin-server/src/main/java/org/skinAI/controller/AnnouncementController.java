package org.skinAI.controller;

import org.skinAI.mapper.AnnouncementMapper;
import org.skinAI.pojo.Announcement;
import org.skinAI.pojo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementController(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @GetMapping
    public Result<List<Announcement>> listEnabled() {
        return Result.success(announcementMapper.selectEnabled());
    }

    @PostMapping
    public Result<Announcement> create(@RequestBody Announcement announcement) {
        if (announcement.getStatus() == null || announcement.getStatus().isBlank()) {
            announcement.setStatus("DISABLED");
        }
        announcementMapper.insert(announcement);
        return Result.success(announcement);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable("id") Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        int affected = announcementMapper.update(announcement);
        if (affected > 0) {
            return Result.success();
        }
        return Result.error("announcement not found");
    }
}
