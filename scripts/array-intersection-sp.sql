create or replace function ST_IntersectionArray(geoms geometry[]) returns geometry as $$
declare
  i integer;
  tmpGeom geometry;
begin
  tmpGeom := geoms[1];
  FOR i IN 1..array_length(geoms,1) LOOP
    tmpGeom:= ST_Intersection(tmpGeom,geoms[i]);
  END LOOP;
  return tmpGeom;
end;
$$
LANGUAGE plpgsql;