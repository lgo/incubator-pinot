package com.linkedin.thirdeye.hadoop.util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.pinot.common.data.DimensionFieldSpec;
import com.linkedin.pinot.common.data.FieldSpec;
import com.linkedin.pinot.common.data.FieldSpec.DataType;
import com.linkedin.pinot.common.data.FieldSpec.FieldType;
import com.linkedin.pinot.common.data.MetricFieldSpec;
import com.linkedin.pinot.common.data.Schema;
import com.linkedin.pinot.common.data.TimeFieldSpec;
import com.linkedin.pinot.common.data.TimeGranularitySpec;
import com.linkedin.thirdeye.api.DimensionSpec;
import com.linkedin.thirdeye.api.MetricSpec;
import com.linkedin.thirdeye.api.StarTreeConstants;
import com.linkedin.thirdeye.hadoop.ThirdEyeConfig;

public class ThirdeyePinotSchemaUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(ThirdeyePinotSchemaUtils.class);


  public static Schema createSchema(ThirdEyeConfig thirdeyeConfig) {
    Schema schema = new Schema();
    for (DimensionSpec dimensionSpec : thirdeyeConfig.getDimensions()) {
      FieldSpec fieldSpec = new DimensionFieldSpec();
      fieldSpec.setName(dimensionSpec.getName());
      fieldSpec.setFieldType(FieldType.DIMENSION);
      fieldSpec.setDataType(DataType.STRING);
      fieldSpec.setSingleValueField(true);
      schema.addField(dimensionSpec.getName(), fieldSpec);
    }
    boolean countIncluded = false;
    for (MetricSpec metricSpec : thirdeyeConfig.getMetrics()) {
      FieldSpec fieldSpec = new MetricFieldSpec();
      String metricName = metricSpec.getName();
      if (metricName.equals(StarTreeConstants.AUTO_METRIC_COUNT)) {
        countIncluded = true;
      }
      fieldSpec.setName(metricName);
      fieldSpec.setFieldType(FieldType.METRIC);
      fieldSpec.setDataType(DataType.valueOf(metricSpec.getType().toString()));
      fieldSpec.setSingleValueField(true);
      schema.addField(metricName, fieldSpec);
    }
    if (!countIncluded) {
      FieldSpec fieldSpec = new MetricFieldSpec();
      String metricName = StarTreeConstants.AUTO_METRIC_COUNT;
      fieldSpec.setName(metricName);
      fieldSpec.setFieldType(FieldType.METRIC);
      fieldSpec.setDataType(DataType.LONG);
      fieldSpec.setDefaultNullValue(1);
      schema.addField(metricName, fieldSpec);
    }
    TimeGranularitySpec incoming =
        new TimeGranularitySpec(DataType.LONG,
            thirdeyeConfig.getTime().getBucket().getSize(),
            thirdeyeConfig.getTime().getBucket().getUnit(),
            thirdeyeConfig.getTime().getColumnName());
    TimeGranularitySpec outgoing =
        new TimeGranularitySpec(DataType.LONG,
            thirdeyeConfig.getTime().getBucket().getSize(),
            thirdeyeConfig.getTime().getBucket().getUnit(),
            thirdeyeConfig.getTime().getColumnName());

    FieldSpec fieldSpec = new TimeFieldSpec(incoming, outgoing);
    fieldSpec.setFieldType(FieldType.TIME);
    schema.addField(thirdeyeConfig.getTime().getColumnName(), new TimeFieldSpec(incoming, outgoing));

    schema.setSchemaName(thirdeyeConfig.getCollection());

    return schema;
  }

  public static Schema createSchema(String configPath) throws IOException {
    FileSystem fs = FileSystem.get(new Configuration());

    ThirdEyeConfig thirdeyeConfig = ThirdEyeConfig.decode(fs.open(new Path(configPath)));
    LOGGER.info("{}", thirdeyeConfig);

    return createSchema(thirdeyeConfig);
  }


}
