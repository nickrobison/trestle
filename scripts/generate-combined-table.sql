-- This pretty much doesn't work.

SELECT *

INTO combined_2000

FROM
  ((
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2009' AS tbl_year
     FROM "2009"
     LIMIT 500
   )
   UNION ALL
   (
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2010' AS tbl_year
     FROM "2010"
     LIMIT 500
   )) as c1;

SELECT *

  INTO combined_2011
  FROM (
   (
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2011' AS tbl_year
     FROM "2011"
     LIMIT 500
   )
   UNION ALL
   (
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2012' AS tbl_year
     FROM "2012"
     LIMIT 500
   )
   UNION ALL
   (
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2013' AS tbl_year
     FROM "2013"
     LIMIT 500
   )
   UNION ALL
   (
     SELECT
       gid,
       CAST(adm2_code AS bigint) AS adm2_code,
       adm2_name,
       CAST(str2_year as bigint) as str2_year,
       CAST(exp2_year as bigint) as exp2_year,
       adm1_code,
       adm1_name,
       status,
       disp_area,
       adm0_code,
       adm0_name,
       shape_leng,
       shape_area,
       geom,
       '2014' AS tbl_year
     FROM "2014"
     LIMIT 500
   )
  ) AS c2