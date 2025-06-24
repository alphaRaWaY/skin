package org.mypetstore.mypetstore.pojo.family;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Family {
    private Integer id;
    private Integer userid;
    private  String name;
    private String gender;
    private Integer age;
    private String relation;
}
