package com.prime.prime_app.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTrend {
    private Integer year;
    private Integer month;
    private Double averageScore;
    
    /**
     * Utility method to convert query result objects to PerformanceTrend instances
     * @param objectArrayList List of Object[] from query result
     * @return List of PerformanceTrend objects
     */
    public static List<PerformanceTrend> fromObjectArray(List<Object[]> objectArrayList) {
        List<PerformanceTrend> result = new ArrayList<>();
        
        if (objectArrayList != null) {
            for (Object[] row : objectArrayList) {
                if (row.length == 3) {
                    PerformanceTrend trend = new PerformanceTrend(
                        (Integer) row[0],
                        (Integer) row[1],
                        (Double) row[2]
                    );
                    result.add(trend);
                }
            }
        }
        
        return result;
    }
} 