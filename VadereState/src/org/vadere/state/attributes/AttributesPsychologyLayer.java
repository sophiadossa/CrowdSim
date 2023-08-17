package org.vadere.state.attributes;

import org.vadere.state.attributes.models.psychology.cognition.*;
import org.vadere.state.attributes.models.psychology.perception.AttributesMultiPerceptionModel;
import org.vadere.state.attributes.models.psychology.perception.AttributesSimplePerceptionModel;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.reflection.VadereAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


// This class beautifies the JSON content by mapping
// the words "perception" and "cognition" to existing class names.

public class AttributesPsychologyLayer {

    // Constants
    // Watch out: Make sure these classes exist!
    @VadereAttribute(exclude = true)
    public static final String DEFAULT_PERCEPTION_MODEL = "SimplePerceptionModel";
    @VadereAttribute(exclude = true)
    public static final String DEFAULT_COGNITION_MODEL = "SimpleCognitionModel";


    // Variables
    // Both should reference to concrete "IPerception" and "ICognition"
    // implementations! We do not reference them here to avoid cyclic
    // dependencies between state and controller packages
    /**
     * <i>perception</i> parameter is the name of the perception model.
     * <br><br>
     * A perception model decides which {@link Stimulus} is most important for a
     * {@link Pedestrian} at a specific simulation step based on pedestrian's
     * attributes or its surrounding. It is designed as an interface so that
     * different models can be used for different scenarios
     * <br><br>
     * Available models:
     * <ul>
     *     <li>"SimplePerceptionModel" {@link AttributesSimplePerceptionModel}</li>
     *     <li>"MultiPerceptionModel" {@link AttributesMultiPerceptionModel}</li>
     * </ul>
     */
    private String perception;
    /**
     * <i>cognition</i> parameter is the name of the cognition model.
     * <br><br>
     * A cognition model decides to which {@link SelfCategory} a {@link Pedestrian}
     * identifies to. From this {@link SelfCategory} a specific behavior derives.
     * E.g. if {@Link SelfCategory} = {@link SelfCategory#COOPERATIVE}, pedestrians
     * can swap places.
     * <br><br>
     * Available models:
     * <ul>
     *     <li>"ChangeTargetScriptedCognitionModel" {@link AttributesChangeTargetScriptedCognitionModel}</li>
     *     <li>"CooperativeCognitionModel" {@link AttributesCooperativeCognitionModel}</li>
     *     <li>"CounterflowCognitionModel" {@link AttributesCounterflowCognitionModel}</li>
     *     <li>"ProbabilisticCognitionModel" {@link AttributesProbabilisticCognitionModel}</li>
     *     <li>"SimpleCognitionModel" {@link AttributesSimpleCognitionModel}</li>
     *     <li>"SocialDistancingCognitionModel" {@link AttributesSocialDistancingCognitionModel}</li>
     *     <li>"ThreatCognitionModel" {@link AttributesThreatCognitionModel}</li>
     */
    private String cognition;
    /**
     * <i>attributesModel</i> parameter is a list of attributes for the perception and cognition models.
     */
    public List<Attributes> attributesModel;

    // Constructors
    public AttributesPsychologyLayer() {
        this(DEFAULT_PERCEPTION_MODEL, DEFAULT_COGNITION_MODEL, new ArrayList<>());
        this.attributesModel.add(0, new AttributesSimplePerceptionModel());
        this.attributesModel.add(1, new AttributesSimpleCognitionModel());
    }

    public AttributesPsychologyLayer(String perception, String cognition, List<Attributes> attributesModel ) {
        this.perception = perception;
        this.cognition = cognition;
        this.attributesModel = attributesModel;
    }

    // Getter
    public String getPerception() {
        return perception;
    }

    public String getCognition() {
        return cognition;
    }

    // Setter
    public void setPerception(String perception) {
        this.perception = perception;
    }

    public void setCognition(String cognition) {
        this.cognition = cognition;
    }

    // Overridden Methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributesPsychologyLayer that = (AttributesPsychologyLayer) o;
        return Objects.equals(perception, that.perception) &&
                Objects.equals(cognition, that.cognition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perception, cognition);
    }


    public List<Attributes> getAttributesModel() {
        return attributesModel;
    }

    public void setAttributesModel(List<Attributes> attributesModel) {
        this.attributesModel = attributesModel;
    }



}
