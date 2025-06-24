// Report.java
package org.mypetstore.mypetstore.pojo.report;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Report {
    private Long id;

    // 表单信息
    private String username;
    private String gender;
    private Integer age;
    private String symptoms;
    private String duration;
    private String treatment;
    private String other;
    private LocalDateTime checkTime;

    // 图片 URL
    private String imageUrl;

    // 结果信息
    private String diseaseType;
    private String value;
    private String advice;
    private String introduction;

}
