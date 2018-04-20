# Check for the necessary commands
EXECUTABLES = wget unzip docker csvsql shp2pgsql mvn
K := $(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),some string,$(error "No $(exec) in PATH")))

DATA_DIR := data
DATASETS := census king_county gates_test
CENSUS_YEARS := $(shell seq 2011 1 2015)
GAUL_RELEASE := 2015
GAUL_YEARS := $(shell seq 1990 1 2014)
DB_HOST := localhost
DATABASE := tiger
DB_USER := nickrobison
DB_PASSWORD := ""

.PHONY: cloc
cloc:
	cloc --exclude-dir=$(shell tr '\n' ',' < ./.clocignore) .

# Docker commands

.PHONY: graphdb
graphdb:
	docker build --build-arg version=8.5.0 -t docker.nickrobison.com:5000/graphdb:8.5.0-free docker/graphdb

.PHONY: evaluation
evaluation: graphdb
	docker build -t docker.nickrobison.com:5000/evaluation-data docker/evaluation-docker

.PHONY: nginx
nginx:
	docker build -t docker.nickrobison.com:5000/nginx-proxy docker/nginx-proxy

.PHONY: docker
docker: nginx evaluation

# Build data directories
.PHONY: prep-dir
prep-dir:
	-@mkdir -p $(DATA_DIR)/census
	-@mkdir -p $(DATA_DIR)/tiger
	-@mkdir -p $(DATA_DIR)/king_county
	-@mkdir -p $(DATA_DIR)/tiger_kc
	-@mkdir -p $(DATA_DIR)/gaul
	-@mkdir -p $(DATA_DIR)/gates_test

# Data for king county (Spatial Intersection Test)
.PHONY: king_county
king_county: $(DATA_DIR)/king_county/kc_tract_10.shp $(DATA_DIR)/tiger_kc/tl_2010_53033_tract10.shp

$(DATA_DIR)/king_county/kc.zip:
	@wget https://www.seattle.gov/Documents/Departments/OPCD/Demographics/GeographicFilesandMaps/KingCountyTractsShapefiles.zip -O $@

$(DATA_DIR)/king_county/kc_tract_10.shp: $(DATA_DIR)/king_county/kc.zip
	@unzip -o $^ -d $(dir $@)

$(DATA_DIR)/tiger_kc/tiger_kc.zip:
	@wget ftp://ftp2.census.gov/geo/tiger/TIGER2010/TRACT/2010/tl_2010_53033_tract10.zip -O $@

$(DATA_DIR)/tiger_kc/tl_2010_53033_tract10.shp: $(DATA_DIR)/tiger_kc/tiger_kc.zip
	@unzip -o $^ -d $(dir $@)

# Census data
.PHONY: census
census: $(DATA_DIR)/tiger/.pop_loaded $(CENSUS_YEARS)
	-@rm -rf data/tiger/out/

.SECONDEXPANSION:
$(CENSUS_YEARS): $(DATA_DIR)/census/acs_$$@.zip $(DATA_DIR)/tiger/.tl_$$@_us_county.loaded

$(DATA_DIR)/tiger/.pop_loaded:
	csvsql --db postgresql://$(DB_HOST):5432/$(DATABASE) --table population --insert -e latin1 trestle-integrations/src/test/resources/co-est2015-alldata.csv
	@touch $@

split_name = $(subst _, , $(basename $(notdir $@)))

# Figure out the year from the census census file names
census_year = $(word 2, $(split_name))

$(DATA_DIR)/census/acs_%.zip:
	@wget https://www2.census.gov/programs-surveys/acs/summary_file/$(census_year)/data/1_year_entire_sf/All_Geographies.zip -O $@

# Save these files because they're really big, and the Census server is slow
.PRECIOUS: $(DATA_DIR)/tiger/tl_%_us_county.zip
$(DATA_DIR)/tiger/tl_%_us_county.zip:
	@wget ftp://ftp2.census.gov/geo/tiger/TIGER$(census_year)/COUNTY/tl_$(census_year)_us_county.zip -O $@

$(DATA_DIR)/tiger/out/tl_%_us_county.shp: $(DATA_DIR)/tiger/tl_%_us_county.zip
	unzip $^ -d $(dir $@)

county_file = $(subst .,, $(basename $@))

$(DATA_DIR)/tiger/.tl_%_us_county.loaded: $(DATA_DIR)/tiger/out/tl_%_us_county.shp
	@shp2pgsql -I -s 4269 -W LATIN1 $(dir $@)out/$(notdir $(county_file)).shp public.shp$(census_year) | psql -h $(DB_HOST) -t $(DATABASE) -U $(DB_USER)
	@touch $@

.PHONY: gaul $(GAUL_YEARS)
gaul: $(GAUL_YEARS)

# Generate GAUL rules, since we have this nested folder structure
gaul_simple = $(basename $(notdir $@))
gaul_split = $(subst _, , $(gaul_simple))
gaulr = $(subst g,, $(word 1, $(gaul_split)))
gaul_year = $(word 2, $(gaul_split))

define build_gaul
$(DATA_DIR)/gaul/g$(GAUL_RELEASE)_$1_2/g$(GAUL_RELEASE)_$1_2.shp:
#	We need the double $$ in order to prevent initial expansion when calling eval
	$$(error "Unable to automatically get year $(1) for release $(GAUL_RELEASE)")
endef

$(foreach f,$(GAUL_YEARS),$(eval $(call build_gaul,$f)))

.SECONDEXPANSION:
$(GAUL_YEARS): $(DATA_DIR)/gaul/g$(GAUL_RELEASE)_$$@_2/g$(GAUL_RELEASE)_$$@_2.shp

trestle-tools/target/trestle-tools.jar:
	@mvn clean package -pl trestle-tools

$(DATA_DIR)/gates_test/.done:
	#	Run the subsetter to extract only Uganda and Mozambique
	@java -jar trestle-tools/target/trestle-tools.jar $(DATA_DIR)/gaul $(DATA_DIR)/gates_test 253 170
	@touch $@

# GAUL datasets
.PHONY: gates_test
gates_test: gaul trestle-tools/target/trestle-tools.jar $(DATA_DIR)/gates_test/.done


.PHONY: data
data: prep-dir $(DATASETS)
