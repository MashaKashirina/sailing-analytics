package com.sap.sailing.windestimation.data;

public interface SimpleManeuverWithEstimatedType<T extends SimpleManeuverForEstimation> {

    ManeuverTypeForClassification getManeuverType();

    T getManeuver();

    double getConfidence();

}
