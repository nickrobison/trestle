-- DROP TABLE Objects;
CREATE TABLE Objects (
  ObjectID   UUID,
  GaulCode   BIGINT,
  ObjectName VARCHAR(255),
  StartDate  DATE,
  EndDate    DATE,
  Geom       GEOMETRY
)