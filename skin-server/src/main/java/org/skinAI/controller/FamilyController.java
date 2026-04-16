package org.skinAI.controller;

import org.skinAI.mapper.UserMapper;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.family.Family;
import org.skinAI.services.FamilyService;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FamilyService familyService;
    @GetMapping
    public Result<List<Family>> getFamily() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        return Result.success(familyService.findAllFamilies(userid));
    }
    @DeleteMapping("/{name}")
    public Result deleteFamily(@PathVariable String name) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        System.out.println(name);
        if(familyService.deleteFamily(name,userid))return Result.success();
        else return Result.error("删除失败");
    }

    @PostMapping
    public Result addFamily(@RequestBody Family family) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        family.setUserid(userid);
        familyService.addFamily(family);
        return Result.success();
    }

    @PutMapping
    public Result updateFamily(@RequestBody Family family) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        Family family1 = familyService.findByName(family.getName(),userid);
        if(family1 == null) {
            return Result.error("编辑的家人不存在");
        }
        else
        {
            family1.setName(family.getName());
            family1.setGender(family.getGender());
            family1.setAge(family.getAge());
            family1.setRelation(family.getRelation());
            System.out.println(family1);
            familyService.updateFamily(family1);
        }
        return Result.success();
    }
}
