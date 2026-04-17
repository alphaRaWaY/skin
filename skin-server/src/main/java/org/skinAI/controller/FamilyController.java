package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.pojo.family.Family;
import org.skinAI.services.FamilyService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping
    public Result<List<Family>> getFamily() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        return Result.success(familyService.findAllFamilies(userid));
    }

    @DeleteMapping("/{name}")
    public Result deleteFamily(@PathVariable String name) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        if (familyService.deleteFamily(name, userid)) {
            return Result.success();
        }
        return Result.error("Delete family failed");
    }

    @PostMapping
    public Result addFamily(@RequestBody Family family) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        family.setUserid(userid);
        familyService.addFamily(family);
        return Result.success();
    }

    @PutMapping
    public Result updateFamily(@RequestBody Family family) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        Family existed = familyService.findByName(family.getName(), userid);
        if (existed == null) {
            return Result.error("Family not found");
        }

        existed.setName(family.getName());
        existed.setGender(family.getGender());
        existed.setAge(family.getAge());
        existed.setRelation(family.getRelation());
        familyService.updateFamily(existed);
        return Result.success();
    }
}
