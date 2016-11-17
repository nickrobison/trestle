package com.nickrobison.trestle.ontology.util;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by detwiler on 10/29/16.
 */
public class ImportsConfigReader {
    private String mappingsFileName = "ontology/importMappingConfigs.json";
    private String importsDirectoryPath = null;
    private Map<IRI,String> iriToFilenameMap = new HashMap<IRI,String>();

    public void readConfigFile() {
        ClassLoader classLoader = getClass().getClassLoader();

        String userDir = System.getProperty("user.home");
        importsDirectoryPath = userDir+"ontology/imports/";
        System.err.println("user home directory : "+importsDirectoryPath);

        // the OWLAPI currently fails to load from this inner-jar IRI
        //importsDirectoryPath = classLoader.getResource("ontology/imports/").getPath();

        InputStream configFileStream = null;
        try {
            configFileStream = classLoader.getResourceAsStream(mappingsFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject obj = JSON.parse(configFileStream);
        JsonValue dirVal = obj.get("importsDirectory");

        if(dirVal!=null&&!dirVal.isNull()&&dirVal.isString()) {
            importsDirectoryPath = dirVal.getAsString().value();
        }

        JsonValue mappingsVal = obj.get("mappings");
        if(mappingsVal!=null && mappingsVal.isArray())
        {
            JsonArray mappingsArray = mappingsVal.getAsArray();
            Iterator<JsonValue> mappingsIt = mappingsArray.iterator();
            while(mappingsIt.hasNext())
            {
                JsonValue mappingVal = mappingsIt.next();
                //TODO: this next section needs more type checks
                if(mappingVal.isObject())
                {
                    JsonObject mappingObj = mappingVal.getAsObject();
                    String iriString = mappingObj.get("iri").getAsString().value();
                    String fileString = mappingObj.get("file").getAsString().value();
                    IRI iri = IRI.create(iriString);
                    iriToFilenameMap.put(iri,fileString);
                }
            }
        }
    }

    public String getImportsDirectoryPath() {
        return importsDirectoryPath;
    }

    public Map<IRI, String> getIriToFilenameMap() {
        return iriToFilenameMap;
    }

    public static void main(String[] args)
    {
        ImportsConfigReader reader = new ImportsConfigReader();
        reader.readConfigFile();
        System.err.println(reader.getImportsDirectoryPath());
        System.err.println(reader.getIriToFilenameMap());
    }
}
