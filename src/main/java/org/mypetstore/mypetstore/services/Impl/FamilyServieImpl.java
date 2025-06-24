package org.mypetstore.mypetstore.services.Impl;

import org.mypetstore.mypetstore.mapper.FamilyMapper;
import org.mypetstore.mypetstore.pojo.family.Family;
import org.mypetstore.mypetstore.services.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class FamilyServieImpl implements FamilyService {
    @Autowired
    private FamilyMapper familyMapper;
    @Override
    public List<Family> findAllFamilies(Integer userid) {
        return familyMapper.findByUserid(userid);
    }

    @Override
    public boolean deleteFamily(String name, Integer userid) {
        return familyMapper.deleteByName(name,userid);
    }

    @Override
    public void addFamily(Family family) {
        familyMapper.insert(family);
    }

    @Override
    public Family findByName(String name,Integer userid) {
        return familyMapper.findByName(name,userid);

    }

    @Override
    public Family findByRelation(String relation, Integer userid) {
        System.out.println(relation);
        System.out.println(userid);
        return familyMapper.findByRelationAndUserid(relation,userid);
    }

    @Override
    public void updateFamily(Family family1) {
        familyMapper.update(family1);
    }
}
