import com.nickrobison.trestle.annotations.DataProperty;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.Ignore;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * Created by nrobison on 6/27/16.
 */
@OWLClassName(className="GAUL_Test")
public class GAULTestClass {

    @DataProperty(name="ADM0_Code", datatype=OWL2Datatype.XSD_INTEGER)
    public int adm0_code;
    public String adm0_name;
    @IndividualIdentifier
    @Ignore
    public String test_name;

    GAULTestClass() {
        this.adm0_code = 12;
        this.adm0_name = "test object";
        this.test_name = "test me";
    }

    GAULTestClass(int code, String name) {
        this.adm0_code = code;
        this.adm0_name = name;
        this.test_name = "test_me";
    }
}
