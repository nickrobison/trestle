(ns trestle.compiler.trestle.compiler.janino
  (:import
    (org.codehaus.commons.compiler Location)
    (org.codehaus.janino Unparser Java$CompilationUnit Java$AbstractCompilationUnit$SingleTypeImportDeclaration Java$PackageDeclaration Java$PackageMemberClassDeclaration Java$AccessModifier Java$Type)
    (java.io OutputStreamWriter)))


(defn class-builder
  [_spec]
  (let [
        loc (Location. "", 0, 0)
        imports [(Java$AbstractCompilationUnit$SingleTypeImportDeclaration. loc (into-array String ["com.nickrobison"]))]
        body (Java$CompilationUnit. nil (into-array Java$AbstractCompilationUnit$SingleTypeImportDeclaration imports))
        clazz (Java$PackageMemberClassDeclaration.
                loc "This is a javadoc"
                (into-array Java$AccessModifier [(Java$AccessModifier. "pubic" loc)])
                "TestClass1234"
                nil, nil, (into-array Java$Type []))
        ]
    (.setPackageDeclaration body (Java$PackageDeclaration. loc "com.nickrobison.test"))
    (.addPackageMemberTypeDeclaration body clazz)
    (Unparser/unparse body (OutputStreamWriter. System/out))
    ))
