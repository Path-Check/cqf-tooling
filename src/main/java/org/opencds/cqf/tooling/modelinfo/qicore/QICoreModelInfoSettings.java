package org.opencds.cqf.tooling.modelinfo.qicore;

import java.util.ArrayList;

import org.hl7.elm_modelinfo.r1.ConversionInfo;
import org.opencds.cqf.tooling.modelinfo.ModelInfoSettings;

public class QICoreModelInfoSettings extends ModelInfoSettings {

    public QICoreModelInfoSettings(String version) {
        super("QICore", version, "http://hl7.org/fhir/us/qicore", "Patient", "birthDate", "qicore");
    }
}