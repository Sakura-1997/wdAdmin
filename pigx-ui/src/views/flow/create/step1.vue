<script setup lang="ts">
import { computed, onMounted, reactive, ref, useTemplateRef, watch } from 'vue';
import type { FormInstance, FormItemRule, FormRules } from 'element-plus';
import { Avatar, ArrowDown, InfoFilled } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';
import SelectShow from '/@/components/OrgSelector/index.vue';
import { queryGroupList } from '/@/api/flow/group';
import type { GroupVO } from '/@/api/flow/group/types';
import { validateFlowId } from '/@/api/flow/flow';
import { useFlowStore } from '../workflow/stores/flow';

interface ValidationError {
	message?: string;
}

type ValidationFields = Record<string, ValidationError[]>;

const props = defineProps<{
	groupId?: string;
}>();

const route = useRoute();
const flowStore = useFlowStore();
const ruleForm = useTemplateRef<FormInstance>('ruleForm');
const advancedSettings = useTemplateRef<HTMLDetailsElement>('advancedSettings');
const groupList = ref<GroupVO[]>([]);

const form = computed(() => flowStore.step1);

const isEditMode = computed(() => Boolean((route.params.flowId || route.query.flowId) && route.query.cp !== '1'));

const validateFlowIdAsync = async (_rule: FormItemRule, value: string): Promise<void> => {
	if (isEditMode.value || !value || !/^[a-zA-Z][a-zA-Z0-9]*$/.test(value)) {
		return;
	}

	const { data } = await validateFlowId(value);
	if (data !== true && data?.exists !== true) {
		throw new Error('流程ID不存在');
	}
};

const rules = reactive<FormRules>({
	name: [
		{ required: true, message: '请填写名称', trigger: 'blur' },
		{ min: 2, max: 10, message: '2-10个字符', trigger: 'blur' },
	],
	flowId: [
		{ required: false, trigger: 'blur' },
		{
			pattern: /^[a-zA-Z][a-zA-Z0-9]*$/,
			message: '流程ID只能包含英文字母和数字，且必须以字母开头',
			trigger: 'blur',
		},
		{ min: 1, max: 50, message: '1-50个字符', trigger: 'blur' },
		{ validator: validateFlowIdAsync, trigger: 'blur' },
	],
	remark: [
		{ required: false, message: '请填写描述', trigger: 'blur' },
		{ min: 2, max: 20, message: '2-20个字符', trigger: 'blur' },
	],
	groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
	logo: [{ required: true, message: '请上传图标', trigger: 'change' }],
	adminList: [{ required: true, message: '请选择管理员', trigger: 'change' }],
});

const validate = (): Promise<void> => {
	return new Promise((resolve, reject) => {
		if (advancedSettings.value) {
			advancedSettings.value.open = true;
		}

		if (!ruleForm.value) {
			reject(['基础信息表单尚未初始化']);
			return;
		}

		void ruleForm.value.validate((valid, fields) => {
			if (valid) {
				resolve();
				return;
			}

			const validationFields = fields as ValidationFields | undefined;
			const messages = Object.values(validationFields ?? {}).flatMap((errors) => {
				const message = errors[0]?.message;
				return message ? [message] : [];
			});
			reject(messages);
		});
	});
};

defineExpose({ validate });

onMounted(async () => {
	const { data } = await queryGroupList();
	groupList.value = data;
	if (!form.value.groupId && data.length > 0) {
		form.value.groupId = data[0].id;
	}
});

watch(
	() => props.groupId,
	(value) => {
		if (value !== undefined) {
			form.value.groupId = value;
		}
	}
);
</script>

<template>
	<section class="flow-info-page" aria-labelledby="flow-info-title">
		<div class="flow-info-panel">
			<h2 id="flow-info-title" class="flow-info-title">流程信息</h2>

			<el-form ref="ruleForm" class="flow-info-form" :model="form" :rules="rules" label-position="top" status-icon scroll-to-error @submit.prevent>
				<div class="flow-info-primary">
					<div class="logo-column">
						<el-form-item :label="$t('flow.logo')" prop="logo" class="logo-form-item">
							<div class="logo-control">
								<upload-img v-model:imageUrl="form.logo" class="logo-uploader" height="148px" width="148px" border-radius="10px">
									<template #empty>
										<div class="logo-placeholder">
											<el-icon><Avatar /></el-icon>
											<span>{{ $t('flow.logo') }}</span>
										</div>
									</template>
								</upload-img>
								<p class="field-help">支持 JPG、PNG 格式，建议 1:1，大小不超过 5MB</p>
							</div>
						</el-form-item>
					</div>

					<div class="main-fields">
						<div class="field-grid">
							<el-form-item :label="$t('flow.name')" prop="name">
								<el-input v-model="form.name" maxlength="10" show-word-limit placeholder="请输入流程名称" />
							</el-form-item>

							<el-form-item :label="$t('flow.group')" prop="groupId">
								<el-select v-model="form.groupId" class="field-control" :placeholder="$t('flow.groupTips')">
									<el-option v-for="item in groupList" :key="item.id" :label="item.groupName" :value="item.id" />
								</el-select>
							</el-form-item>
						</div>

						<el-form-item :label="$t('flow.remark')" prop="remark">
							<el-input
								v-model="form.remark"
								type="textarea"
								:rows="3"
								maxlength="20"
								show-word-limit
								resize="none"
								placeholder="请简要说明流程的适用场景"
							/>
						</el-form-item>

						<el-form-item prop="adminList" class="admin-form-item">
							<template #label>
								<span>{{ $t('flow.admin') }}</span>
								<el-tooltip content="管理员可维护并发布该流程" placement="top">
									<el-icon class="label-tip" aria-label="管理员说明" tabindex="0"><InfoFilled /></el-icon>
								</el-tooltip>
							</template>
							<div class="admin-selector">
								<SelectShow v-model:orgList="form.adminList" type="user" :multiple="false" />
							</div>
						</el-form-item>
					</div>
				</div>

				<details ref="advancedSettings" class="advanced-settings" open>
					<summary class="advanced-summary">
						<el-icon class="advanced-arrow"><ArrowDown /></el-icon>
						<span>高级设置</span>
						<span class="advanced-optional">（可选）</span>
					</summary>

					<div class="advanced-content">
						<el-form-item prop="flowId" class="flow-id-form-item">
							<template #label>
								<span>{{ $t('flow.flowId') }}</span>
								<Tip :content="$t('flow.flowIdTip')" />
							</template>
							<el-input v-model="form.flowId" maxlength="50" :placeholder="$t('flow.flowIdTip')" :readonly="isEditMode" />
							<p class="field-help flow-id-help">不填写将自动生成，发布后不可修改</p>
						</el-form-item>
					</div>
				</details>
			</el-form>
		</div>
	</section>
</template>

<style scoped>
.flow-info-page {
	box-sizing: border-box;
	width: 100%;
	padding: 0;
	overflow: auto;
	background: var(--el-bg-color);
}

.flow-info-panel {
	box-sizing: border-box;
	width: 100%;
	min-height: 100%;
	margin: 0;
	padding: 28px clamp(24px, 3vw, 48px) 30px;
	color: var(--el-text-color-primary);
	background: var(--el-bg-color);
	border: 0;
	border-radius: 0;
	box-shadow: none;
}

.flow-info-title {
	margin: 0 0 28px;
	font-size: 20px;
	font-weight: 600;
	line-height: 28px;
	color: var(--el-text-color-primary);
}

.flow-info-primary {
	display: grid;
	grid-template-columns: 320px minmax(0, 1fr);
	gap: 38px;
}

.logo-column,
.main-fields {
	min-width: 0;
}

.logo-form-item {
	margin-bottom: 22px;
}

.logo-control {
	display: grid;
	gap: 12px;
}

.logo-placeholder {
	display: flex;
	flex-direction: column;
	gap: 8px;
	align-items: center;
	justify-content: center;
	font-size: 13px;
	color: var(--el-text-color-secondary);
}

.logo-placeholder .el-icon {
	font-size: 28px;
	color: var(--el-color-primary);
}

.field-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 20px 28px;
}

.field-control {
	width: 100%;
}

.field-help {
	max-width: 230px;
	margin: 0;
	font-size: 12px;
	line-height: 20px;
	color: var(--el-text-color-secondary);
}

.label-tip {
	margin-left: 6px;
	font-size: 14px;
	color: var(--el-text-color-secondary);
	cursor: help;
}

.admin-form-item {
	margin-bottom: 4px;
}

.admin-selector {
	box-sizing: border-box;
	width: 100%;
	min-height: 54px;
	padding: 6px 10px;
	background: var(--el-fill-color-blank);
	border: 1px solid var(--el-border-color);
	border-radius: var(--el-border-radius-base);
}

.admin-selector :deep(> div) {
	display: flex;
	align-items: center;
	gap: 8px;
	min-height: 40px;
}

.admin-selector :deep(> div > div:first-child) {
	display: contents;
}

.admin-selector :deep(> div > div:last-child) {
	order: 1;
	flex: 1;
	width: auto !important;
	margin-top: 0 !important;
}

.admin-selector :deep(> div > .el-button) {
	order: 2;
}

.advanced-settings {
	margin-top: 30px;
	padding-top: 24px;
	border-top: 1px solid var(--el-border-color-lighter);
}

.advanced-summary {
	display: inline-flex;
	gap: 6px;
	align-items: center;
	font-size: 15px;
	font-weight: 600;
	line-height: 24px;
	color: var(--el-text-color-primary);
	cursor: pointer;
	list-style: none;
}

.advanced-summary::-webkit-details-marker {
	display: none;
}

.advanced-summary:focus-visible {
	outline: 2px solid var(--el-color-primary-light-5);
	outline-offset: 4px;
	border-radius: var(--el-border-radius-small);
}

.advanced-arrow {
	font-size: 14px;
	transition: transform var(--el-transition-duration);
}

.advanced-settings:not([open]) .advanced-arrow {
	transform: rotate(-90deg);
}

.advanced-optional {
	font-size: 13px;
	font-weight: 400;
	color: var(--el-text-color-secondary);
}

.advanced-content {
	padding-top: 18px;
}

.flow-id-form-item {
	width: min(100%, 500px);
	margin-bottom: 0;
}

.flow-id-help {
	max-width: none;
	margin-top: 8px;
}

.flow-info-form :deep(.el-form-item__label) {
	padding-bottom: 8px;
	font-weight: 500;
	line-height: 20px;
	color: var(--el-text-color-primary);
}

.flow-info-form :deep(.el-form-item) {
	margin-bottom: 22px;
}

.flow-info-form :deep(.el-input__wrapper),
.flow-info-form :deep(.el-select__wrapper) {
	min-height: 42px;
}

.flow-info-form :deep(.el-textarea__inner) {
	min-height: 88px !important;
	padding: 11px 12px;
	line-height: 22px;
}

.logo-uploader :deep(.el-upload--picture-card),
.logo-uploader :deep(.el-upload-list__item) {
	background: var(--el-fill-color-blank);
	border-color: var(--el-border-color);
}

.logo-uploader :deep(.el-upload--picture-card:hover) {
	background: var(--el-color-primary-light-9);
	border-color: var(--el-color-primary);
}

.admin-selector :deep(.el-button) {
	width: 38px;
	height: 38px;
	color: var(--el-color-primary);
	background: var(--el-color-primary-light-9);
	border-color: var(--el-color-primary-light-7);
}

.admin-selector :deep(.el-button:hover) {
	color: var(--el-color-white);
	background: var(--el-color-primary);
	border-color: var(--el-color-primary);
}

@media (max-width: 1100px) {
	.flow-info-primary {
		grid-template-columns: 220px minmax(0, 1fr);
		gap: 28px;
	}

	.field-grid {
		gap: 18px;
	}
}

@media (max-width: 820px) {
	.flow-info-panel {
		padding: 22px 20px;
	}

	.flow-info-primary {
		grid-template-columns: 1fr;
		gap: 24px;
	}

	.logo-column {
		display: grid;
		grid-template-columns: 148px minmax(0, 230px);
		gap: 18px;
		align-items: end;
	}

	.field-grid {
		grid-template-columns: 1fr;
		gap: 0;
	}
}

@media (max-width: 520px) {
	.flow-info-panel {
		padding: 20px 16px;
	}

	.flow-info-title {
		margin-bottom: 22px;
	}

	.logo-column {
		display: block;
	}

	.logo-column .field-help {
		margin-top: 10px;
	}
}
</style>
