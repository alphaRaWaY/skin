package org.mypetstore.mypetstore.services;

import org.mypetstore.mypetstore.pojo.family.Family;

import java.util.List;

public interface FamilyService {
    List<Family>  findAllFamilies(Integer userid);

    boolean deleteFamily(String name, Integer userid);

    void addFamily(Family family);

    Family findByName(String name,Integer userid);


    Family findByRelation(String relation, Integer userid);

    void updateFamily(Family family1);
}
