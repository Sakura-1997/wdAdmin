<script lang="ts" setup>
import { nextTick, onMounted, shallowRef, useTemplateRef, watch } from 'vue';
import { EditPen, InfoFilled, Link, QuestionFilled } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import FcDesigner from 'form-create-designer';
import type { Config, DragRule } from 'form-create-designer';
import { BpmModelFormType, BpmModelFormTypeOptions } from '../form/const/constants';
import { rules } from '../form/const/form-rules';
import { useFlowStore } from '../workflow/stores/flow';
import { initFormCreateFetch } from '../workflow/utils/formCreateFetch';

initFormCreateFetch();

type DesignerInstance = InstanceType<typeof FcDesigner>;
type ValidationField = { message?: string };

const designerRef = useTemplateRef<DesignerInstance>('designer');
const customFormRef = useTemplateRef<FormInstance>('customForm');
const flowStore = useFlowStore();
const isDesignerInitialized = shallowRef(false);
const isSavingDesigner = shallowRef(false);
const designerInitError = shallowRef('');

const designerConfig: Config = {
	fieldReadonly: false,
	showSaveBtn: true,
	showDevice: true,
	hiddenItem: ['upload'],
	formOptions: {
		resetBtn: {
			show: false,
			innerText: '重置',
		},
		submitBtn: {
			show: false,
			innerText: '提交',
		},
	},
};

const customFormRules: FormRules = {
	formCustomCreatePath: [
		{ required: true, message: '请输入表单提交路径', trigger: 'blur' },
		{ pattern: /^\/.*/, message: '路径必须以"/"开头', trigger: 'blur' },
	],
	formCustomViewPath: [
		{ required: true, message: '请输入表单查看路径', trigger: 'blur' },
		{ pattern: /^\/.*/, message: '路径必须以"/"开头', trigger: 'blur' },
	],
};

const normalizeValidationErrors = (error: unknown): string[] => {
	if (typeof error === 'string') return [error];
	if (!error || typeof error !== 'object') return ['表单验证失败'];

	const messages = Object.values(error as Record<string, ValidationField[]>).flatMap((fields) => {
		if (!Array.isArray(fields)) return [];
		return fields.map((field) => field?.message).filter((message): message is string => Boolean(message));
	});

	return messages.length > 0 ? messages : ['表单验证失败'];
};

const saveFormDesign = () => {
	if (flowStore.step2.formType !== BpmModelFormType.NORMAL || !designerRef.value) return;

	isSavingDesigner.value = true;
	flowStore.step2.formRule = designerRef.value.getRule();
	flowStore.step2.formOption = designerRef.value.getOption();
	nextTick(() => {
		isSavingDesigner.value = false;
	});
};

const handleFormTypeChange = (value: string | number | boolean | undefined) => {
	const nextType = Number(value) as BpmModelFormType;
	if (nextType === flowStore.step2.formType) return;

	if (flowStore.step2.formType === BpmModelFormType.NORMAL) {
		saveFormDesign();
	}

	flowStore.step2.formType = nextType;
	if (nextType === BpmModelFormType.NORMAL) {
		nextTick(initDesigner);
	}
};

const validate = async (): Promise<void> => {
	try {
		if (flowStore.step2.formType === BpmModelFormType.NORMAL) {
			if (designerInitError.value) throw [designerInitError.value];
			if (!designerRef.value) throw ['表单设计器尚未初始化，请稍后重试'];

			const rule = designerRef.value.getRule();
			if (rule.length === 0) throw ['请设计表单内容'];
			flowStore.step2.formRule = rule;
			return;
		}

		if (flowStore.step2.formType === BpmModelFormType.CUSTOM) {
			if (!customFormRef.value) throw ['自定义表单尚未初始化，请稍后重试'];
			const customErrors: string[] = [];
			const createPath = flowStore.step2.formCustomCreatePath?.trim() ?? '';
			const viewPath = flowStore.step2.formCustomViewPath?.trim() ?? '';
			if (!createPath) customErrors.push('请输入表单提交路径');
			else if (!createPath.startsWith('/')) customErrors.push('表单提交路径必须以"/"开头');
			if (!viewPath) customErrors.push('请输入表单查看路径');
			else if (!viewPath.startsWith('/')) customErrors.push('表单查看路径必须以"/"开头');
			if (customErrors.length > 0) {
				void customFormRef.value.validate().catch(() => undefined);
				throw customErrors;
			}
			try {
				await customFormRef.value.validate();
			} catch (error: unknown) {
				throw normalizeValidationErrors(error);
			}
			return;
		}

		throw ['请选择表单类型'];
	} catch (error: unknown) {
		if (Array.isArray(error)) throw error;
		throw [error instanceof Error ? error.message : '表单验证失败'];
	}
};

const initDesigner = async () => {
	if (!designerRef.value) return;

	await nextTick();

	try {
		if (!isDesignerInitialized.value) {
			designerRef.value.addMenu({ name: 'biz', title: '业务组件', list: [], before: true });
			rules.forEach((rule) => designerRef.value?.addComponent(rule as DragRule));
			isDesignerInitialized.value = true;
		}

		if (flowStore.step2.formRule?.length) designerRef.value.setRule(flowStore.step2.formRule);
		if (flowStore.step2.formOption) designerRef.value.setOption(flowStore.step2.formOption);
		designerInitError.value = '';
	} catch (error) {
		designerInitError.value = error instanceof Error ? error.message : '表单设计器初始化失败，请刷新页面后重试';
	}
};

onMounted(() => {
	nextTick(initDesigner);
});

watch(
	() => flowStore.step2.formRule,
	() => {
		if (isSavingDesigner.value || flowStore.step2.formType !== BpmModelFormType.NORMAL) return;
		nextTick(initDesigner);
	}
);

defineExpose({ validate, saveFormDesign });
</script>

<template>
	<section class="form-design-page" aria-label="表单设计">
		<div class="form-type-section">
			<el-radio-group :model-value="flowStore.step2.formType" class="form-type-grid" aria-label="选择表单类型" @change="handleFormTypeChange">
				<el-radio
					v-for="option in BpmModelFormTypeOptions"
					:key="option.value"
					:value="option.value"
					class="form-type-option"
					:data-testid="`form-type-${option.value}`"
				>
					<span class="form-type-content">
						<span class="form-type-icon" aria-hidden="true">
							<el-icon>
								<EditPen v-if="option.value === BpmModelFormType.NORMAL" />
								<Link v-else />
							</el-icon>
						</span>
						<span class="form-type-copy">
							<span class="form-type-title">{{ option.label }}</span>
							<span class="form-type-description">{{ option.description }}</span>
						</span>
					</span>
				</el-radio>
			</el-radio-group>
		</div>

		<div v-show="flowStore.step2.formType === BpmModelFormType.NORMAL" class="designer-workspace">
			<FcDesigner ref="designer" height="100%" :config="designerConfig" @save="saveFormDesign" />
		</div>

		<div v-show="flowStore.step2.formType === BpmModelFormType.CUSTOM" class="custom-form-workspace">
			<div class="custom-form-content">
				<div class="custom-form-heading">
					<span class="custom-form-heading-icon" aria-hidden="true">
						<el-icon><Link /></el-icon>
					</span>
					<div>
						<h2 class="custom-form-title">自定义表单配置</h2>
						<p class="custom-form-subtitle">关联已有业务页面，分别用于发起流程和查看详情。</p>
					</div>
				</div>

				<el-form ref="customForm" :model="flowStore.step2" :rules="customFormRules" label-position="top" class="custom-form">
					<el-form-item label="表单提交路径" prop="formCustomCreatePath">
						<el-input v-model="flowStore.step2.formCustomCreatePath" placeholder="例如：/flow/oaLeave/create.vue" clearable />
						<p class="field-hint">
							<el-icon aria-hidden="true"><InfoFilled /></el-icon>
							用户发起流程时访问的 Vue 路由地址
						</p>
					</el-form-item>

					<el-form-item label="表单查看路径" prop="formCustomViewPath">
						<el-input v-model="flowStore.step2.formCustomViewPath" placeholder="例如：/flow/oaLeave/detail.vue" clearable />
						<p class="field-hint">
							<el-icon aria-hidden="true"><InfoFilled /></el-icon>
							流程实例详情中用于展示表单的 Vue 路由地址
						</p>
					</el-form-item>
				</el-form>

				<div class="usage-note">
					<div class="usage-note-title">
						<el-icon aria-hidden="true"><QuestionFilled /></el-icon>
						使用说明
					</div>
					<ul class="usage-note-list">
						<li>路径须以“/”开头，并指向项目中已创建的 Vue 页面。</li>
						<li>提交路径用于发起流程，查看路径用于流程实例详情展示。</li>
					</ul>
				</div>
			</div>
		</div>
	</section>
</template>

<style lang="scss" scoped>
.form-design-page {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	min-height: 0;
	height: 100%;
	padding: 0;
	background: var(--el-bg-color);
	color: var(--el-text-color-primary);
}

.form-type-section {
	flex: 0 0 auto;
	padding: 12px clamp(16px, 2vw, 28px);
	border-bottom: 1px solid var(--el-border-color-lighter);
	background: var(--el-bg-color);
}

.form-type-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 12px;
	width: min(720px, 100%);
	margin: 0 auto;
}

:deep(.form-type-option) {
	position: relative;
	display: flex;
	align-items: center;
	width: 100%;
	height: 62px;
	margin: 0;
	padding: 10px 44px 10px 14px;
	border: 1px solid var(--el-border-color);
	border-radius: var(--el-border-radius-base);
	background: var(--el-bg-color);
	transition: border-color var(--el-transition-duration), background-color var(--el-transition-duration), box-shadow var(--el-transition-duration);
}

:deep(.form-type-option:hover) {
	border-color: var(--el-color-primary-light-5);
	background: var(--el-color-primary-light-9);
}

:deep(.form-type-option:focus-within) {
	box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}

:deep(.form-type-option.is-checked) {
	border-color: var(--el-color-primary);
	background: var(--el-color-primary-light-9);
	box-shadow: inset 0 0 0 1px var(--el-color-primary);
}

:deep(.form-type-option .el-radio__input) {
	position: absolute;
	top: 50%;
	right: 16px;
	margin: 0;
	transform: translateY(-50%);
}

:deep(.form-type-option .el-radio__label) {
	width: 100%;
	padding: 0;
	color: inherit;
}

.form-type-content {
	display: flex;
	align-items: center;
	gap: 12px;
	min-width: 0;
}

.form-type-icon {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 34px;
	height: 34px;
	border-radius: var(--el-border-radius-base);
	background: var(--el-fill-color-light);
	color: var(--el-text-color-secondary);
	font-size: 18px;
}

:deep(.form-type-option.is-checked) .form-type-icon {
	background: var(--el-color-primary-light-8);
	color: var(--el-color-primary);
}

.form-type-copy {
	display: flex;
	min-width: 0;
	flex-direction: column;
	gap: 2px;
}

.form-type-title {
	font-size: 14px;
	font-weight: 600;
	line-height: 20px;
	color: var(--el-text-color-primary);
}

.form-type-description {
	overflow: hidden;
	font-size: 12px;
	line-height: 18px;
	color: var(--el-text-color-secondary);
	text-overflow: ellipsis;
	white-space: nowrap;
}

.designer-workspace {
	flex: 1;
	min-width: 0;
	min-height: 0;
	overflow: hidden;
	border: 0;
	border-radius: 0;
	background: var(--el-bg-color);
}

:deep(._fc-designer) {
	height: 100% !important;
	min-height: 0;
	background: var(--el-bg-color);
	--fc-tool-border-color: var(--el-color-primary);
}

:deep(._fc-l-menu),
:deep(._fc-l),
:deep(._fc-m),
:deep(._fc-r),
:deep(._fc-m-tools),
:deep(._fc-r-tabs) {
	border-color: var(--el-border-color-light);
	background: var(--el-bg-color);
	color: var(--el-text-color-primary);
}

:deep(._fc-l-close),
:deep(._fc-r-close),
:deep(._fc-l-open),
:deep(._fc-r-open) {
	background: var(--el-bg-color-overlay);
	color: var(--el-text-color-regular);
}

:deep(._fc-m-con) {
	background: var(--el-bg-color-page);
}

:deep(._fc-l-title),
:deep(._fc-r-title),
:deep(._fc-r ._fc-r-tab) {
	color: var(--el-text-color-primary);
}

:deep(._fc-l-item) {
	border-color: var(--el-border-color-light);
	background: var(--el-bg-color);
	color: var(--el-text-color-regular);
}

:deep(._fc-l-item:hover) {
	background: var(--el-color-primary);
	color: var(--el-color-white);
}

:deep(._fc-m-tools .line),
:deep(._fc-r .el-tabs__nav-wrap::after) {
	background: var(--el-border-color-light);
}

:deep(._fc-m-tools ._fd-m-extend) {
	border-color: var(--el-border-color);
	background: var(--el-fill-color-light);
	color: var(--el-text-color-regular);
}

.custom-form-workspace {
	flex: 1;
	min-height: 0;
	overflow: auto;
	padding: 0;
	background: var(--el-bg-color);
}

.custom-form-content {
	box-sizing: border-box;
	width: 100%;
	min-height: 100%;
	margin: 0;
	padding: 28px clamp(24px, 3vw, 48px);
	border: 0;
	border-radius: 0;
	background: var(--el-bg-color);
}

.custom-form-heading {
	display: flex;
	align-items: center;
	gap: 12px;
	padding-bottom: 20px;
	border-bottom: 1px solid var(--el-border-color-lighter);
}

.custom-form-heading-icon {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 38px;
	height: 38px;
	border-radius: var(--el-border-radius-base);
	background: var(--el-color-primary-light-9);
	color: var(--el-color-primary);
	font-size: 18px;
}

.custom-form-title {
	margin: 0;
	font-size: 16px;
	font-weight: 600;
	line-height: 24px;
}

.custom-form-subtitle {
	margin: 2px 0 0;
	font-size: 13px;
	line-height: 20px;
	color: var(--el-text-color-secondary);
}

.custom-form {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 0 20px;
	margin-top: 24px;
}

:deep(.custom-form .el-form-item__label) {
	font-weight: 500;
	color: var(--el-text-color-primary);
}

.field-hint {
	display: flex;
	align-items: center;
	gap: 5px;
	margin: 6px 0 0;
	font-size: 12px;
	line-height: 18px;
	color: var(--el-text-color-secondary);
}

.usage-note {
	margin-top: 4px;
	padding: 14px 16px;
	border: 1px solid var(--el-color-primary-light-8);
	border-radius: var(--el-border-radius-base);
	background: var(--el-color-primary-light-9);
}

.usage-note-title {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 13px;
	font-weight: 600;
	color: var(--el-text-color-primary);
}

.usage-note-list {
	display: grid;
	gap: 4px;
	margin: 8px 0 0;
	padding-left: 20px;
	font-size: 12px;
	line-height: 20px;
	color: var(--el-text-color-regular);
}

@media (max-width: 960px) {
	.designer-workspace {
		overflow: auto;
	}

	.custom-form {
		grid-template-columns: 1fr;
	}
}

@media (max-width: 640px) {
	.form-type-grid {
		grid-template-columns: 1fr;
	}

	.form-type-section {
		padding-block: 10px;
	}

	.custom-form-content {
		padding: 20px 16px;
	}
}
</style>
