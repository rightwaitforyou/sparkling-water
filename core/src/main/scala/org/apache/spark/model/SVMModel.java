/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.model;

import hex.Model;
import hex.ModelMetrics;
import hex.ModelMetricsBinomial;
import org.apache.spark.mllib.linalg.Vectors;
import water.H2O;
import water.Key;
import water.api.KeyV3;

import java.util.Arrays;

public class SVMModel extends Model<SVMModel, SVMModel.SVMParameters, SVMModel.SVMOutput> {

    public static class SVMParameters extends Model.Parameters {
        public String algoName() {
            return "SVM";
        }

        public String fullName() {
            return "Support Vector Machine";
        }

        public String javaName() {
            return SVMModel.class.getName();
        }

        @Override
        public long progressUnits() {
            return _max_iterations;
        }

        public int _max_iterations = 1000; // Max iterations
        public String training_rdd;
        public double _step_size;
        public double _reg_param;
        public double _mini_batch_fraction;
        public String _optimizer;
        public boolean _add_intercept;
        public KeyV3.FrameKeyV3 _user_points; // TODO this should be either frame or Spark DataFrame
    }

    public static class SVMOutput extends Model.Output {
        // Iterations executed
        public int _iterations;
        public double interceptor;
        public double[] weights;
        public long[] _training_time_ms = new long[]{System.currentTimeMillis()};

        public SVMOutput(SVM b) {
            super(b);
        }

    }

    SVMModel(Key selfKey, SVMModel.SVMParameters parms, SVMModel.SVMOutput output) {
        super(selfKey, parms, output);
    }

    @Override
    public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
        return new ModelMetricsBinomial.MetricBuilderBinomial(domain);
    }

    @Override
    protected double[] score0(double data[/*ncols*/], double preds[/*nclasses+1*/]) {
        org.apache.spark.mllib.classification.SVMModel model =
                new org.apache.spark.mllib.classification.SVMModel(Vectors.dense(_output.weights), _output.interceptor);
        // TODO should this return only the predicted class or two values {X, Y} for 0 and 1?
        // TODO2 this probably shouldnt copyOfRange, should I assume data of correct size?
        preds[0] = model.predict(Vectors.dense(Arrays.copyOfRange(data, 1, data.length)));
        return preds;
    }
}