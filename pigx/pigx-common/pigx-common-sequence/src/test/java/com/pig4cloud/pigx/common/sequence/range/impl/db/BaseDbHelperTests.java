package com.pig4cloud.pigx.common.sequence.range.impl.db;

import org.anyline.data.param.ConfigStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDbHelperTests {

    @Test
    void updateRangeMatchesNameAndValue() {
        ConfigStore config = BaseDbHelper.updateRangeConfig(10L, "order");

        assertThat(config.getConfigValue(SequenceTable.Fields.value)).isEqualTo(10L);
        assertThat(config.getConfigValue(SequenceTable.Fields.name)).isEqualTo("order");
    }

}
