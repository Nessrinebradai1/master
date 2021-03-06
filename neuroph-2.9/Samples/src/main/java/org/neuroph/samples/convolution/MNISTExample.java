package org.neuroph.samples.convolution;

import org.neuroph.contrib.model.errorestimation.KFoldCrossValidation;
import org.neuroph.contrib.model.errorestimation.KFoldCrossValidation;
import org.neuroph.contrib.eval.Evaluation;
import org.neuroph.contrib.eval.classification.ClassificationMetrics;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.core.learning.error.MeanSquaredError;
import org.neuroph.nnet.ConvolutionalNetwork;
import org.neuroph.nnet.comp.Kernel;
import org.neuroph.nnet.comp.layer.Layer2D;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.samples.convolution.mnist.MNISTDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Konvolucioni parametri
 * <p/>
 * Globalna arhitektura: Konvolucioni i pooling lejeri - naizmenicno (samo konvolucioni ili naizmenicno konvolccioni pooling)
 * Za svaki lajer da ima svoj kernel (mogu svi konvolucioni da imaju isti kernel, ili svi pooling isti kernel)
 * da mogu da zadaju neuron properties (transfer funkciju) za konvolucioni i pooling lejer(input)
 * Konektovanje lejera? - po defaultu full connect (ostaviti api za custom konekcije)
 * <p/>
 * addFeatureMaps...
 * connectFeatureMaps
 * <p/>
 * Helper utility klasa...
 * <p/>
 * Osnovni kriterijumi:
 * 1. Jednostavno kreiranje default neuronske mreze
 * 2. Laka customizacija i kreiranje custom arhitektura: konvolucionih i pooling lejera i transfer/input funkcija
 * Napraviti prvo API i ond aprilagodti kod
 * <p/>
 * ------------------------
 * <p/>
 * promeniti nacin kreiranja i dodavanja feature maps layera
 * resiti InputMaps Layer, overridovana metoda koja baca unsupported exception ukazuje da nesto nije u redu sa dizajnom
 * Da li imamo potrebe za klasom kernel  - to je isto kao i dimension?
 * <p/>
 * zasto je public abstract void connectMaps apstraktna? (u klasi FeatureMapsLayer)
 * <p/>
 * InputMapsLayer konstruktoru superklase prosledjuje null...
 * <p/>
 * fullConnectMapLayers
 *
 * 
 * Same as CNNMNIST just with hardoced params
 * 
 * @author zoran
 */


public class MNISTExample {

    private static Logger LOG = LoggerFactory.getLogger(MNISTExample.class);


    static class LearningListener implements LearningEventListener {

        private final NeuralNetwork neuralNetwork;
        private DataSet testSet;

        public LearningListener(NeuralNetwork neuralNetwork, DataSet testSet) {
            this.testSet = testSet;
            this.neuralNetwork = neuralNetwork;
        }


        long start = System.currentTimeMillis();

        public void handleLearningEvent(LearningEvent event) {
            BackPropagation bp = (BackPropagation) event.getSource();
            LOG.error("Current iteration: " + bp.getCurrentIteration());
            LOG.error("Error: " + bp.getTotalNetworkError());
            LOG.error("" + (System.currentTimeMillis() - start) / 1000.0);
            neuralNetwork.save(bp.getCurrentIteration() + "CNN_MNIST" + bp.getCurrentIteration() + ".nnet");
            start = System.currentTimeMillis();
//            NeuralNetworkEvaluationService.completeEvaluation(neuralNetwork, testSet);
        }

    }

    public static void main(String[] args) {
        try {

            DataSet trainSet = MNISTDataSet.createFromFile(MNISTDataSet.TRAIN_LABEL_NAME, MNISTDataSet.TRAIN_IMAGE_NAME, 60000);
            DataSet testSet = MNISTDataSet.createFromFile(MNISTDataSet.TEST_LABEL_NAME, MNISTDataSet.TEST_IMAGE_NAME, 10000);

            Layer2D.Dimensions inputDimension = new Layer2D.Dimensions(32, 32);
            Kernel convolutionKernel = new Kernel(5, 5);
            Kernel poolingKernel = new Kernel(2, 2);

            ConvolutionalNetwork convolutionNetwork = new ConvolutionalNetwork.ConvolutionalNetworkBuilder(inputDimension, 1)
                    .withConvolutionLayer(convolutionKernel, 10)
                    .withPoolingLayer(poolingKernel)
                    .withConvolutionLayer(convolutionKernel, 1)
                    .withPoolingLayer(poolingKernel)
                    .withConvolutionLayer(convolutionKernel, 1)
                    .withFullConnectedLayer(10)
                    .createNetwork();

            BackPropagation backPropagation = new MomentumBackpropagation();
            backPropagation.setLearningRate(0.0001);
            backPropagation.setMaxError(0.00001);
            backPropagation.setMaxIterations(50);
            backPropagation.addListener(new LearningListener(convolutionNetwork, testSet));
            backPropagation.setErrorFunction(new MeanSquaredError());

            convolutionNetwork.setLearningRule(backPropagation);
            KFoldCrossValidation crossValidation = new KFoldCrossValidation(convolutionNetwork, testSet, 6);
            
            
           // ClassificationMetrics validationResult = crossValidation.computeErrorEstimate(convolutionNetwork, trainSet);

            Evaluation.runFullEvaluation(convolutionNetwork, testSet);
            convolutionNetwork.save("/mnist.nnet");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
