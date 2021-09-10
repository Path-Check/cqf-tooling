package org.opencds.cqf.tooling.utilities;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.NamespaceInfo;
import org.cqframework.cql.cql2elm.NamespaceManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.fhir.ucum.UcumService;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Measure;
import org.opencds.cqf.tooling.operation.ExtractMatBundleOperation;
import org.testng.annotations.Test;
import org.opencds.cqf.tooling.measure.MeasureRefreshProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class ECQMCreatorTest {
    private static ModelManager modelManager;
    private static LibraryManager libraryManager;
    private static UcumService ucumService;

    private static Logger logger = LoggerFactory.getLogger(ECQMCreatorTest.class);

    private static FhirContext context = FhirContext.forR5();

    private Measure refreshMeasure(String primaryLibraryPath, String measurePath) throws IOException {
        CqlTranslatorOptions cqlTranslatorOptions = new CqlTranslatorOptions();
        cqlTranslatorOptions.getFormats().add(CqlTranslator.Format.JSON);
        cqlTranslatorOptions.getOptions().add(CqlTranslator.Options.EnableAnnotations);
        // This option performs data analysis, including element reference detection
        cqlTranslatorOptions.setAnalyzeDataRequirements(true);
        // This option collapses duplicate data requirements
        cqlTranslatorOptions.setCollapseDataRequirements(true);
        CqlTranslator translator = createTranslator(primaryLibraryPath, cqlTranslatorOptions);
        translator.toELM();
        cacheLibrary(translator.getTranslatedLibrary());
        IParser parser = measurePath.endsWith(".json") ? context.newJsonParser() : context.newXmlParser();
        InputStream inputStream = this.getClass().getResourceAsStream(measurePath);
        Measure measureToConvert = parser.parseResource(Measure.class, inputStream);
        MeasureRefreshProcessor refreshProcessor = new MeasureRefreshProcessor();
        Measure returnMeasure = refreshProcessor.refreshMeasure(measureToConvert, libraryManager, translator.getTranslatedLibrary(), cqlTranslatorOptions);
        return returnMeasure;
    }

    private String measureToString(Measure measure) {
        IParser parser = context.newJsonParser().setPrettyPrint(true);
        String measureResourceContent = parser.encodeResourceToString(measure);
        return measureResourceContent;
    }

    private  List<DataRequirement> StartMatOutputTest(String matBundleName, String measureLibraryName){

        System.out.println("Testing " + matBundleName + " " + measureLibraryName);

        Boolean outputMeasure2File = true; 

        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/" + matBundleName).getFile() });

        List<DataRequirement> drs = new ArrayList<DataRequirement>();
        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/" + measureLibraryName +".cql", "ecqm-content-r4-2021/input/resources/measure/"+ measureLibraryName +".json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }

            String measString = measureToString(measure);

            if (outputMeasure2File){
                try (PrintWriter measWriter = new PrintWriter("./src/test/java/org/opencds/cqf/tooling/output/"+"post_processed_" + matBundleName);) {
                    measWriter.println(measString);
                }
            }
            
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measString);
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return drs;
    }


    @Test
    public void TestCMS125FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS125FHIR-v0-0-004-FHIR-4-0-1.json", "BreastCancerScreeningsFHIR");
        // TODO: Measure-specific validation of data requirements content
    }

    @Test
    public void TestCMS104FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS104-v2-0-004-FHIR-4-0-1.json", "DischargedonAntithromboticTherapyFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS130FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS130FHIR-v0-0-002-FHIR-4-0-1.json", "ColorectalCancerScreeningsFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS347FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS347FHIR-v0-1-013-FHIR-4-0-1.json", "FHIR347");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestHybridHWRFHIR() {
        List<DataRequirement> drs = StartMatOutputTest("HybridHWRFHIR-v1-3-001-FHIR-4-0-1.json", "HybridHWRFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestHybridHWMFHIR() {
        List<DataRequirement> drs = StartMatOutputTest("HybridHWMFHIR-v0-101-021-FHIR-4-0-1.json", "HybridHWMFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestSafeUseofOpioidsFHIR() {
        List<DataRequirement> drs = StartMatOutputTest("Safe Use of OpioidsFHIR-v0-0-009-FHIR-4-0-1.json", "SafeUseofOpioidsConcurrentPrescribingFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestIPSSwithBPHdxFHIR() {
        List<DataRequirement> drs = StartMatOutputTest("IPSS with BPH dxFHIR-v0-0-004-FHIR-4-0-1.json", "UrinarySymptomScoreChange6to12MonthsAfterDiagnosisofBenignProstaticHyperplasiaFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestHIVScreeningFHIR() {
        List<DataRequirement> drs = StartMatOutputTest("HIV ScreeningFHIR-v0-0-003-FHIR-4-0-1.json", "HIVScreeningFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestDEXAScreenProstateFHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("DEXA screen prostateFHIR-v0-0-005-FHIR-4-0-1.json", "BoneDensityProstateCancerAndrogenDeprivationTherapyFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS249FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS249FHIR-v0-0-001-FHIR-4-0-1.json", "AppropriateDXAScansForWomenUnder65FHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS177FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS177FHIR-v0-0-002-FHIR-4-0-1.json", "ChildandAdolescentMajorDepressiveDisorderMDDSuicideRiskAssessmentFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS165FHIR() {

        List<DataRequirement> drs = StartMatOutputTest("CMS165FHIR-v0-0-003-FHIR-4-0-1.json", "ControllingHighBloodPressureFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS161FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS161FHIR-v0-0-002-FHIR-4-0-1.json", "AdultMajorDepressiveDisorderMDDSuicideRiskAssessmentFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS157FHIR() {

        List<DataRequirement> drs = StartMatOutputTest("CMS157FHIR-v0-0-006-FHIR-4-0-1.json", "OncologyPainIntensityQuantifiedFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS156FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS156FHIR-v0-0-001-FHIR-4-0-1.json", "UseofHighRiskMedicationsintheElderlyFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS155FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS155FHIR-v0-0-002-FHIR-4-0-1.json", "WeightAssessmentandCounselingforNutritionandPhysicalActivityforChildrenandAdolescentsFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS154FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS154FHIR-v0-0-001-FHIR-4-0-1.json", "AppropriateTreatmentforUpperRespiratoryInfectionURIFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS153FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS153FHIR-v0-0-002-FHIR-4-0-1.json", "ChlamydiaScreeningforWomenFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS149FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS149FHIR-v0-0-002-FHIR-4-0-1.json", "DementiaCognitiveAssessmentFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS147FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS147FHIR-v0-0-002-FHIR-4-0-1.json", "PreventiveCareandScreeningInfluenzaImmunizationFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS146FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS146FHIR-v0-0-001-FHIR-4-0-1.json", "AppropriateTestingforPharyngitisFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS145FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS145FHIR-v0-0-007-FHIR-4-0-1.json", "CADBetaBlockerTherapyPriorMIorLVSDFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS144FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS144FHIR-v0-0-005-FHIR-4-0-1.json", "HFBetaBlockerTherapyforLVSDFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS143FHIR() {

        List<DataRequirement> drs = StartMatOutputTest("CMS143FHIR-v0-0-001-FHIR-4-0-1.json", "POAGOpticNerveEvaluationFHIR");
        // TODO: Measure-specific validation of data requirements content

    }
    
    @Test
    public void TestCMS139FHIR() {
        
        List<DataRequirement> drs = StartMatOutputTest("CMS139FHIR-v0-0-001-FHIR-4-0-1.json", "FallsScreeningforFutureFallRiskFHIR");
        // TODO: Measure-specific validation of data requirements content
    
    }
    
    @Test
    public void TestCMS138FHIR() { 
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template

        List<DataRequirement> drs = StartMatOutputTest("CMS138FHIR-v0-0-001-FHIR-4-0-1.json", "PreventiveCareandScreeningTobaccoUseScreeningandCessationInterventionFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS137FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS137FHIR-v0-0-001-FHIR-4-0-1.json", "InitiationandEngagementofAlcoholandOtherDrugDependenceTreatmentFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS136FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS136FHIR-v0-0-001-FHIR-4-0-1.json", "FollowUpCareforChildrenPrescribedADHDMedicationADDFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS135FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS135FHIR-v0-0-013-FHIR-4-0-1.json", "HFACEIorARBorARNIforLVSDFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS134FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS134FHIR-v0-0-001-FHIR-4-0-1.json", "DiabetesMedicalAttentionforNephropathyFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS133FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS133FHIR-v0-0-009-FHIR-4-0-1.json", "Cataracts2040BCVAwithin90DaysFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS131FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS131FHIR-v0-0-003-FHIR-4-0-1.json", "DiabetesEyeExamFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS129FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS129FHIR-v0-0-005-FHIR-4-0-1.json", "ProstateCaAvoidanceBoneScanOveruseFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS128FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS128FHIR-v0-0-001-FHIR-4-0-1.json", "AntidepressantMedicationManagementFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS127FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS127FHIR-v0-0-001-FHIR-4-0-1.json", "PneumococcalVaccinationStatusforOlderAdultsFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS124FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS124FHIR-v0-0-003-FHIR-4-0-1.json", "CervicalCancerScreeningFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS117FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS117FHIR-v0-0-001-FHIR-4-0-1.json", "ChildhoodImmunizationStatusFHIR");
        // TODO: Measure-specific validation of data requirements content
    }
    
    @Test
    public void TestCMS90FHIR() {
        List<DataRequirement> drs = StartMatOutputTest("CMS90FHIR-v0-0-005-FHIR-4-0-1.json", "FunctionalStatusAssessmentsforHeartFailureFHIR");
        // TODO: Measure-specific validation of data requirements content
    }    
    
    @Test
    public void TestCMS75FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS75FHIR-v0-0-001-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/ChildrenWhoHaveDentalDecayorCavitiesFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/ChildrenWhoHaveDentalDecayorCavitiesFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS74FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS74FHIR-v0-0-004-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS72FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS72FHIR-v0-0-003-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/AntithromboticTherapyByEndofHospitalDay2FHIR.cql", "ecqm-content-r4-2021/input/resources/measure/AntithromboticTherapyByEndofHospitalDay2FHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS69FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS69FHIR-v0-0-004-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/PCSBMIScreenAndFollowUpFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/PCSBMIScreenAndFollowUpFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS68FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS68FHIR-v0-0-004-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/DocumentationofCurrentMedicationsFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/DocumentationofCurrentMedicationsFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS66FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS66FHIR-v0-0-002-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/FunctionalStatusAssessmentforTotalKneeReplacementFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/FunctionalStatusAssessmentforTotalKneeReplacementFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS56FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS56FHIR-v0-0-003-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/FunctionalStatusAssessmentforTotalHipReplacementFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/FunctionalStatusAssessmentforTotalHipReplacementFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS50FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS50FHIR-v0-0-013-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/CRLReceiptofSpecialistReportFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/CRLReceiptofSpecialistReportFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS22FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS22FHIR-v0-0-007-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/PreventiveBPScreeningFollowUpFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/PreventiveBPScreeningFollowUpFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }

            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestCMS2FHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS2FHIR-v0-0-002-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/PCSDepressionScreenAndFollowUpFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/PCSDepressionScreenAndFollowUpFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    @Test
    public void TestBCG4NonmuscleInvasiveBCaFHIR() {
        // Extract the bundle
        // NOTE: This is a 2021-AUFHIR measure, this is the test created using TestCMS125FHIR as template
        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/BCG 4 nonmuscle invasive BCaFHIR-v0-0-006-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/IntravesicalBacillusCalmetteGuerinForNonmuscleInvasiveBladderCancerFHIR.cql", "ecqm-content-r4-2021/input/resources/measure/IntravesicalBacillusCalmetteGuerinForNonmuscleInvasiveBladderCancerFHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestCMS122FHIR() {

        ExtractMatBundleOperation o = new ExtractMatBundleOperation();
        o.execute(new String[] { "-ExtractMATBundle", this.getClass().getResource("ecqm-content-r4-2021/bundles/CMS122FHIR-v0-0-004-FHIR-4-0-1.json").getFile() });

        try {
            Measure measure = refreshMeasure("ecqm-content-r4-2021/input/cql/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR.cql", "ecqm-content-r4-2021/input/resources/measure/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR.json");
            assertTrue(null != measure);
            // Extract data requirements from the measure:
            List<DataRequirement> drs = new ArrayList<DataRequirement>();
            for (Extension e : measure.getExtensionsByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-dataRequirement")) {
                if (e.hasValue()) {
                    drs.add(e.getValueDataRequirement());
                }
            }
            assertTrue(!drs.isEmpty());
            // TODO: Measure-specific validation of data requirements content
            logger.debug(measureToString(measure));
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestBCSComponent() {
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/BCSComponent.cql", "CompositeMeasures/resources/BCSComponent-v0-0-001-FHIR-4-0-1.xml");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestCCSComponent() {
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/CCSComponent.cql", "CompositeMeasures/resources/CCSComponent-v0-0-001-FHIR-4-0-1.xml");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestHBPComponent() {
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/HBPComponent.cql", "CompositeMeasures/resources/HBPComponent-v0-0-001-FHIR-4-0-1.xml");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestPVSComponent() {
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/PVSComponent.cql", "CompositeMeasures/resources/PVSComponent-v0-0-001-FHIR-4-0-1.xml");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestTSCComponent() {
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/TSCComponent.cql", "CompositeMeasures/resources/TSCComponent-v0-0-001-FHIR-4-0-1.xml");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Test
    public void TestECQMCreator() {
        // TODO - translate measure into ELM measure then call creator with that measure
        try {
            Measure measure = refreshMeasure("CompositeMeasures/cql/EXM124-9.0.000.cql", "/ecqm/resources/measure-EXM124-9.0.000.json");
            assertTrue(null != measure);
            logger.debug(measureToString(measure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

/*
        Set<String> expressions = new HashSet<>();
        expressions.add("Conditions Indicating End of Life or With Limited Life Expectancy");
        expressions.add("Active Ambulatory Opioid Rx");
        expressions.add("Active Ambulatory Benzodiazepine Rx");
        expressions.add("Active Ambulatory Naloxone Rx");
        expressions.add("Admitted/Referred/Discharged to Hospice Care");
        org.hl7.fhir.r5.model.Library modDefLibrary =  getModuleDefinitionLibrary(expressions);
        assertTrue(null != modDefLibrary);

        Measure newMeasure = eCQMCreator.create_eCQMFromLibrary(modDefLibrary);
        String measureString = parser.setPrettyPrint(true).encodeResourceToString(newMeasure);
*/
    }

    @Test
    public void TestECQMCreatorDataRequirements() {
        // TODO - translate measure into ELM measure then call creator with that measure
        CqlTranslatorOptions cqlTranslatorOptions = new CqlTranslatorOptions();
        cqlTranslatorOptions.getFormats().add(CqlTranslator.Format.JSON);
        cqlTranslatorOptions.getOptions().add(CqlTranslator.Options.EnableAnnotations);
        cqlTranslatorOptions.setCollapseDataRequirements(true);
        String libraryPath = "CompositeMeasures/cql/BCSComponent.cql"; //EXM124-9.0.000.cql";//library-EXM124-9.0.000.json";
        try {
            CqlTranslator translator = createTranslator(libraryPath, cqlTranslatorOptions);
            translator.toELM();
            cacheLibrary(translator.getTranslatedLibrary());
            FhirContext context = FhirContext.forR5();
            IParser parser = context.newJsonParser();
            IParser xmlParser = context.newXmlParser();
            InputStream inputStream = this.getClass().getResourceAsStream("CompositeMeasures/resources/BCSComponent-v0-0-001-FHIR-4-0-1.xml");  //"/ecqm/resources/measure-EXM124-9.0.000.json");
            Measure measureToConvert = xmlParser.parseResource(Measure.class, inputStream);

//            Measure measureToConvert = parser.parseResource(Measure.class, inputStream);
            MeasureRefreshProcessor refreshProcessor = new MeasureRefreshProcessor();
            Measure returnMeasure = refreshProcessor.refreshMeasure(measureToConvert, libraryManager, translator.getTranslatedLibrary(), cqlTranslatorOptions);
            assertTrue(null != returnMeasure);
            logger.debug(parser.setPrettyPrint(true).encodeResourceToString(returnMeasure));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void cacheLibrary(TranslatedLibrary library) {
        // Add the translated library to the library manager (NOTE: This should be a "cacheLibrary" call on the LibraryManager, available in 1.5.3+)
        // Without this, the data requirements processor will try to load the current library, resulting in a re-translation
        String libraryPath = NamespaceManager.getPath(library.getIdentifier().getSystem(), library.getIdentifier().getId());
        libraryManager.getTranslatedLibraries().put(libraryPath, library);
    }

    private static void tearDown() {
        ucumService = null;
        libraryManager = null;
        modelManager = null;
    }

    public static void reset() {
        tearDown();
    }

    private static void setup(String relativePath) {
        modelManager = new ModelManager();
        libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(new DefaultLibrarySourceProvider(Paths.get(relativePath)));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        try {
            ucumService = new UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
        }
        catch (UcumException e) {
            e.printStackTrace();
        }
    }

    private static ModelManager getModelManager() {
        if (modelManager == null) {
            setup(null);
        }

        return modelManager;
    }

    private static LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            setup(null);
        }

        return libraryManager;
    }

    private static UcumService getUcumService() {
        if (ucumService == null) {
            setup(null);
        }

        return ucumService;
    }

    public static CqlTranslator createTranslator(String testFileName, CqlTranslatorOptions options) throws IOException {
        return createTranslator(null, testFileName, options);
    }

    public static CqlTranslator createTranslator(NamespaceInfo namespaceInfo, String testFileName, CqlTranslator.Options... options) throws IOException {
        return createTranslator(namespaceInfo, testFileName, new CqlTranslatorOptions(options));
    }

    public static CqlTranslator createTranslator(NamespaceInfo namespaceInfo, String libraryName, CqlTranslatorOptions options) throws IOException {
        File translationTestFile = new File(ECQMCreatorTest.class.getResource(libraryName).getFile());
        reset();
        setup(translationTestFile.getParent());
        CqlTranslator translator = CqlTranslator.fromFile(namespaceInfo, translationTestFile, getModelManager(), getLibraryManager(), getUcumService(), options);
        return translator;
    }

}
