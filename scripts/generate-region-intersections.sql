SELECT
  adm2_name,
  adm2_code,
  record_num,
  avg_area,
  ST_Area(intersected)   AS intersect_area,
  st_perimeter(intersected) AS intersect_length
FROM (
       SELECT
         adm2_name,
         adm2_code,
         AVG(shape_area)                                             AS avg_area,
         AVG(shape_leng)                                             AS avg_leng,
         COUNT(adm2_name)                                            AS record_num,
         ST_IntersectionArray(ARRAY(SELECT geom
                                    FROM combined
                                    WHERE adm2_name = c1.adm2_name)) AS intersected
       FROM combined AS c1
       GROUP BY adm2_name, adm2_code
     ) AS c2