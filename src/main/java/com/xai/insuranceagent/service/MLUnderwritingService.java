package com.xai.insuranceagent.service;

import com.xai.insuranceagent.model.underwriting.CustomerRiskProfile;
import com.xai.insuranceagent.model.underwriting.UnderwritingDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;

/**
 * Machine Learning based underwriting using Weka Decision Tree
 * Optional service - only loaded if ML is enabled
 */
@Service
@ConditionalOnProperty(name = "insurance.underwriting.use-ml", havingValue = "true")
public class MLUnderwritingService {

    private static final Logger logger = LoggerFactory.getLogger(MLUnderwritingService.class);

    private J48 decisionTree;
    private Instances trainingDataStructure;

    @PostConstruct
    public void initializeMLModel() {
        logger.info("Initializing ML model (Weka Decision Tree)...");
        
        try {
            // Create dataset structure
            createDatasetStructure();
            
            // Train the model with sample data
            trainModel();
            
            logger.info("ML model initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize ML model: {}", e.getMessage(), e);
        }
    }

    /**
     * Assess risk using ML model
     */
    public UnderwritingDecision assessRiskWithML(CustomerRiskProfile riskProfile, 
                                                   UnderwritingDecision decision) {
        logger.debug("Assessing risk with ML model");
        
        try {
            // Create instance from risk profile
            Instance instance = createInstance(riskProfile);
            
            // Classify
            double prediction = decisionTree.classifyInstance(instance);
            double[] distribution = decisionTree.distributionForInstance(instance);
            
            // Interpret results
            interpretMLResults(prediction, distribution, decision);
            
            // Calculate confidence
            double confidence = getMaxConfidence(distribution);
            decision.setConfidenceScore(confidence);
            
            logger.info("ML assessment completed - Prediction: {}, Confidence: {}", 
                    prediction, confidence);
            
            return decision;
            
        } catch (Exception e) {
            logger.error("ML assessment failed: {}", e.getMessage(), e);
            // Fallback to safe decision
            decision.setDecision("REFER");
            decision.setReferralReason("ML assessment failed, manual review required");
            decision.setRequiresManualReview(true);
            return decision;
        }
    }

    /**
     * Create Weka dataset structure
     */
    private void createDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        
        // Numeric attributes
        attributes.add(new Attribute("creditScore"));
        attributes.add(new Attribute("claimsCount"));
        attributes.add(new Attribute("age"));
        attributes.add(new Attribute("yearsLicensed"));
        
        // Nominal attributes
        ArrayList<String> insuranceTypes = new ArrayList<>();
        insuranceTypes.add("auto");
        insuranceTypes.add("home");
        insuranceTypes.add("life");
        insuranceTypes.add("health");
        attributes.add(new Attribute("insuranceType", insuranceTypes));
        
        // Class attribute (decision)
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("APPROVE");
        classValues.add("REJECT");
        classValues.add("REFER");
        attributes.add(new Attribute("decision", classValues));
        
        // Create Instances object
        trainingDataStructure = new Instances("UnderwritingData", attributes, 0);
        trainingDataStructure.setClassIndex(trainingDataStructure.numAttributes() - 1);
    }

    /**
     * Train the model with sample data
     */
    private void trainModel() throws Exception {
        // Create training data
        Instances trainingData = new Instances(trainingDataStructure);
        
        // Add training examples (in production, load from database)
        addTrainingExample(trainingData, 750, 0, 30, 10, "auto", "APPROVE");
        addTrainingExample(trainingData, 800, 0, 35, 15, "auto", "APPROVE");
        addTrainingExample(trainingData, 550, 3, 25, 5, "auto", "REJECT");
        addTrainingExample(trainingData, 500, 5, 22, 3, "auto", "REJECT");
        addTrainingExample(trainingData, 650, 1, 40, 20, "auto", "APPROVE");
        addTrainingExample(trainingData, 620, 2, 28, 8, "auto", "REFER");
        addTrainingExample(trainingData, 700, 0, 45, 25, "home", "APPROVE");
        addTrainingExample(trainingData, 580, 4, 30, 10, "home", "REJECT");
        addTrainingExample(trainingData, 720, 1, 50, 30, "life", "APPROVE");
        addTrainingExample(trainingData, 600, 2, 35, 15, "health", "REFER");
        
        // Initialize and train J48 decision tree
        decisionTree = new J48();
        decisionTree.setOptions(new String[]{"-C", "0.25", "-M", "2"});
        decisionTree.buildClassifier(trainingData);
        
        logger.debug("Decision tree model: \n{}", decisionTree.toString());
    }

    /**
     * Add training example
     */
    private void addTrainingExample(Instances data, double creditScore, double claims, 
                                     double age, double yearsLicensed, 
                                     String insuranceType, String decision) {
        Instance instance = new DenseInstance(6);
        instance.setDataset(data);
        
        instance.setValue(0, creditScore);
        instance.setValue(1, claims);
        instance.setValue(2, age);
        instance.setValue(3, yearsLicensed);
        instance.setValue(4, insuranceType);
        instance.setValue(5, decision);
        
        data.add(instance);
    }

    /**
     * Create instance from risk profile
     */
    private Instance createInstance(CustomerRiskProfile riskProfile) {
        Instance instance = new DenseInstance(6);
        instance.setDataset(trainingDataStructure);
        
        instance.setValue(0, riskProfile.getCreditScore() != null ? riskProfile.getCreditScore() : 650);
        instance.setValue(1, riskProfile.getClaimsInLast3Years() != null ? riskProfile.getClaimsInLast3Years() : 0);
        instance.setValue(2, riskProfile.getAge() != null ? riskProfile.getAge() : 30);
        instance.setValue(3, riskProfile.getYearsLicensed() != null ? riskProfile.getYearsLicensed() : 5);
        instance.setValue(4, riskProfile.getInsuranceType());
        
        return instance;
    }

    /**
     * Interpret ML results
     */
    private void interpretMLResults(double prediction, double[] distribution, 
                                     UnderwritingDecision decision) {
        String predictedClass = trainingDataStructure.classAttribute().value((int) prediction);
        
        decision.setDecision(predictedClass);
        
        switch (predictedClass) {
            case "APPROVE":
                decision.setDecisionReason("ML model recommends approval");
                decision.setRiskLevel("MEDIUM");
                decision.setRiskScore(30);
                decision.setPremiumMultiplier(1.0);
                break;
                
            case "REJECT":
                decision.setDecisionReason("ML model recommends rejection");
                decision.setRiskLevel("VERY_HIGH");
                decision.setRiskScore(90);
                break;
                
            case "REFER":
                decision.setDecisionReason("ML model recommends manual review");
                decision.setReferralReason("Model confidence below threshold");
                decision.setRequiresManualReview(true);
                decision.setRiskLevel("HIGH");
                decision.setRiskScore(65);
                break;
        }
    }

    /**
     * Get maximum confidence from distribution
     */
    private double getMaxConfidence(double[] distribution) {
        double max = 0.0;
        for (double prob : distribution) {
            if (prob > max) {
                max = prob;
            }
        }
        return max;
    }
}

