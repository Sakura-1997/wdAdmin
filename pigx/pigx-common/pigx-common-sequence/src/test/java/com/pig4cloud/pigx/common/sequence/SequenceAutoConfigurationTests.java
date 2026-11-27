package com.pig4cloud.pigx.common.sequence;

import org.anyline.adapter.init.DefaultEnvironmentWorker;
import org.anyline.metadata.Column;
import org.anyline.metadata.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceAutoConfigurationTests {

    @BeforeAll
    static void initAnylineEnvironment() {
        DefaultEnvironmentWorker.start();
    }

    /**
     * Table.from 不会填充 originType，缺失时 DDL 阶段按目标库的类型转换（如
     * PostgreSQL 下 DATETIME -> TIMESTAMP）会被跳过，直接输出 DATETIME 导致建表失败。
     */
    @Test
    void everyColumnCarriesOriginTypeForDialectConversion() {
        Table table = SequenceAutoConfiguration.buildSequenceTable("pigx_sequence");

        for (Object o : table.getColumns().values()) {
            Column column = (Column) o;
            assertThat(column.getOriginType()).as("column %s originType", column.getName())
                .isEqualTo(column.getTypeName());
        }
    }

    /**
     * anyline 随机生成的主键约束名可能以数字开头，未加引号时在 PostgreSQL / MySQL
     * 下都是语法错误，必须显式命名。
     */
    @Test
    void primaryKeyHasExplicitName() {
        Table table = SequenceAutoConfiguration.buildSequenceTable("pigx_sequence");

        assertThat(table.getPrimaryKey()).isNotNull();
        assertThat(table.getPrimaryKey().getName()).isEqualTo("pk_pigx_sequence");
        assertThat(table.getPrimaryKey().getColumns()).containsKey("ID");
    }

}
