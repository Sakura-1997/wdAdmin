<template>
	<el-dialog :close-on-click-modal="false" title="左树右表配置" draggable v-model="visible">
		<el-form :model="form" :rules="dataRules" ref="dataFormRef">
			<el-row :gutter="35">
				<el-col :span="12">
					<el-form-item prop="childTableName">
						<template #label>树表名称<tip content="左侧树所使用的表，例如类目表、组织表" /> </template>
						<el-select placeholder="请选择树表" v-model="form.childTableName" filterable @change="getTreeTableColumnList">
							<el-option :key="item" :label="item" :value="item" v-for="item in childTableList" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12">
					<el-form-item prop="childField">
						<template #label> 关联字段<tip content="右侧主表中关联左侧树节点的字段" /> </template>
						<el-select placeholder="请选择主表关联字段" v-model="form.childField" filterable>
							<el-option :key="item" :label="item" :value="item" v-for="item in mainTableColumnList" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mt-4">
					<el-form-item prop="parentField">
						<template #label>级联字段<tip content="树表结构中表示父级关系的字段，如 parent_id" /> </template>
						<el-select placeholder="请选择父编号字段" v-model="form.parentField" filterable>
							<el-option :key="item" :label="item" :value="item" v-for="item in childTableColumnList" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mt-4">
					<el-form-item prop="nameField">
						<template #label>标题字段<tip content="树表中用于展示名称的字段，如 name" /> </template>
						<el-select placeholder="请选择树名称字段" v-model="form.nameField" filterable>
							<el-option :key="item" :label="item" :value="item" v-for="item in childTableColumnList" />
						</el-select>
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="onClear">清空</el-button>
				<el-button @click="onSubmit" type="primary">{{ $t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script setup lang="ts" name="leftTreeRightTable">
import { useVModel } from '@vueuse/core';
import { useListTableApi, useListTableColumnApi, useSyncTableApi } from '/@/api/gen/table';
import { useMessage } from '/@/hooks/message';
import Tip from '/@/components/Tip/index.vue';

const emit = defineEmits(['update:modelValue']);

type LeftTreeRightTableForm = {
	childTableName: string;
	childField: string;
	parentField: string;
	nameField: string;
};

const props = defineProps<{
	modelValue: LeftTreeRightTableForm;
}>();

const visible = ref(false);
const dataFormRef = ref();
const createDefaultForm = () => ({
	childTableName: '',
	childField: '',
	parentField: '',
	nameField: '',
});

const normalizeForm = (val?: Partial<LeftTreeRightTableForm>) => {
	const source = val || {};
	return {
		childTableName: source.childTableName ?? '',
		childField: source.childField ?? '',
		parentField: source.parentField ?? '',
		nameField: source.nameField ?? '',
	};
};

const form = useVModel(props, 'modelValue', emit, {
	passive: true,
	deep: true,
	clone: true,
	defaultValue: createDefaultForm(),
}) as Ref<LeftTreeRightTableForm>;

const dataRules = ref({
	childTableName: [{ required: true, message: '请选择树表', trigger: 'blur' }],
	childField: [{ required: true, message: '请选择主表关联字段', trigger: 'blur' }],
	parentField: [{ required: true, message: '请选择父编号字段', trigger: 'blur' }],
	nameField: [{ required: true, message: '请选择树名称字段', trigger: 'blur' }],
});

const childTableList = ref<string[]>([]);
const childTableColumnList = ref<string[]>([]);
const mainTableColumnList = ref<string[]>([]);
const currentDsName = ref('');
const currentTableName = ref('');

const openDialog = (row: any) => {
	currentDsName.value = row.dsName;
	currentTableName.value = row.tableName;
	visible.value = true;

	getAllTable(row.dsName);
	getMainTableField(row.dsName, row.tableName);

	form.value = normalizeForm(row);
	if (form.value.childTableName) {
		getTreeTableColumnList(form.value.childTableName);
	}
};

const getAllTable = (dsName: string) => {
	useListTableApi(dsName).then((res) => {
		childTableList.value = res.data
			.map((item: any) => item.name)
			.filter((name: string) => name !== currentTableName.value);
	});
};

const getMainTableField = (dsName: string, tableName: string) => {
	useListTableColumnApi(dsName, tableName).then((res) => {
		mainTableColumnList.value = res.data.map((item: any) => item.name);
	});
};

const getTreeTableColumnList = (tableName: string) => {
	form.value.parentField = '';
	form.value.nameField = '';
	useListTableColumnApi(currentDsName.value, tableName).then((res) => {
		childTableColumnList.value = res.data.map((item: any) => item.name);
		if (!form.value.parentField && childTableColumnList.value.includes('parent_id')) {
			form.value.parentField = 'parent_id';
		}
		if (!form.value.nameField && childTableColumnList.value.includes('name')) {
			form.value.nameField = 'name';
		}
	});
};

const onSubmit = async () => {
	const valid = await dataFormRef.value.validate().catch(() => {});
	if (!valid) return false;

	useSyncTableApi(currentDsName.value, form.value.childTableName).then(() => {
		useMessage().success('左树右表配置成功');
	});
	visible.value = false;
};

const onClear = () => {
	form.value = createDefaultForm();
	visible.value = false;
};

defineExpose({
	openDialog,
});
</script>
