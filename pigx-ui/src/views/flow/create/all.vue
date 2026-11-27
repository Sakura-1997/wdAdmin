<script setup lang="ts">
import { WarnTriangleFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import FcDesigner from 'form-create-designer';
import { computed, nextTick, onMounted, ref, useTemplateRef, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import type { LocationQueryValue } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { addFlow, getFlowDetail } from '/@/api/flow/flow';
import { useUserInfo } from '/@/stores/userInfo';
import other from '/@/utils/other';
import { BpmModelFormType } from '../form/const/constants';
import { useFlowStore } from '../workflow/stores/flow';
import CreateFlowFooter from './components/CreateFlowFooter.vue';
import CreateFlowHeader from './components/CreateFlowHeader.vue';
import Step1 from './step1.vue';
import Step2 from './step2.vue';
import Step3 from './step3.vue';

interface ValidatableStep {
	validate: () => Promise<void>;
}

interface FormDesignStep extends ValidatableStep {
	saveFormDesign: () => void;
}

interface ProcessDesignStep extends ValidatableStep {
	getProcessData: () => Promise<Record<string, unknown>>;
}

const store = useFlowStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const step1Ref = useTemplateRef<ValidatableStep>('step1Ref');
const step2Ref = useTemplateRef<FormDesignStep>('step2Ref');
const step3Ref = useTemplateRef<ProcessDesignStep>('step3Ref');

const steps = [{ title: 'flow.basicInformation' }, { title: 'flow.formDesign' }, { title: 'flow.processDesign' }];
const activeStep = ref(0);
const savedLabel = ref('草稿内容将在页面内保留');
const primaryLabel = computed(() => {
	if (activeStep.value === 0) return `下一步：${t('flow.formDesign')}`;
	if (activeStep.value === 1) return `下一步：${t('flow.processDesign')}`;
	return '发布流程';
});
const previousLabel = computed(() => {
	if (activeStep.value === 0) return '取消';
	return `上一步：${t(steps[activeStep.value - 1].title)}`;
});

const validateErrMsg = ref<string[]>([]);
const validateFlowStep = ref(0);
const validateDialogShow = ref(false);
const validatingShow = ref(false);
const isSubmitting = ref(false);

const paramGroupId = ref<string>();
const step3NodeConfig = ref<Record<string, unknown>>({});

const normalizeErrors = (errors: unknown): string[] => {
	if (Array.isArray(errors)) {
		return errors.map((item) => String(item));
	}
	if (errors instanceof Error) {
		return [errors.message];
	}
	return [String(errors || '校验失败，请检查当前步骤')];
};

const saveCurrentStep = (showMessage = true) => {
	if (activeStep.value === 1) {
		step2Ref.value?.saveFormDesign();
	}
	savedLabel.value = '草稿已保留在当前页面';
	if (showMessage) {
		ElMessage.success('当前步骤草稿已保留');
	}
};

const navigateToStep = (step: number) => {
	if (step < 0 || step >= steps.length || step === activeStep.value) return;
	if (activeStep.value === 1) {
		step2Ref.value?.saveFormDesign();
	}
	activeStep.value = step;
};

const validateCurrentStep = async () => {
	const currentRef = [step1Ref.value, step2Ref.value, step3Ref.value][activeStep.value];
	if (!currentRef) {
		throw new Error('当前步骤尚未初始化，请稍后重试');
	}
	if (activeStep.value === 1) {
		step2Ref.value?.saveFormDesign();
	}
	await currentRef.validate();
};

const handlePrimaryAction = async () => {
	if (activeStep.value === steps.length - 1) {
		publish();
		return;
	}

	try {
		await validateCurrentStep();
		saveCurrentStep(false);
		navigateToStep(activeStep.value + 1);
	} catch (errors) {
		ElMessage.warning(normalizeErrors(errors)[0]);
	}
};

const handlePreviousAction = () => {
	if (activeStep.value === 0) {
		router.push('/flow/list/index');
		return;
	}
	navigateToStep(activeStep.value - 1);
};

const handleValidateError = (errors: unknown) => {
	validatingShow.value = false;
	validateErrMsg.value = normalizeErrors(errors);
};

const validateAllSteps = async () => {
	const validators = [
		() => step1Ref.value?.validate(),
		async () => {
			step2Ref.value?.saveFormDesign();
			await step2Ref.value?.validate();
		},
		() => step3Ref.value?.validate(),
	];

	for (let index = 0; index < validators.length; index += 1) {
		validateFlowStep.value = index;
		try {
			const result = validators[index]();
			if (!result) throw new Error('步骤尚未初始化，请稍后重试');
			await result;
			validateFlowStep.value = index + 1;
			await nextTick();
		} catch (errors) {
			handleValidateError(errors);
			return;
		}
	}

	validatingShow.value = false;
};

const gotoEdit = () => {
	activeStep.value = Math.min(validateFlowStep.value, steps.length - 1);
	validateDialogShow.value = false;
};

function publish() {
	validateErrMsg.value = [];
	validateFlowStep.value = 0;
	validateDialogShow.value = true;
	validatingShow.value = true;
	nextTick(() => void validateAllSteps());
}

const submitFlow = async () => {
	if (isSubmitting.value || !step3Ref.value) return;
	isSubmitting.value = true;

	try {
		step2Ref.value?.saveFormDesign();
		const processData = await step3Ref.value.getProcessData();
		const flow = other.deepClone(store.step1);
		const formConfig = other.deepClone(store.step2);

		if (formConfig.formType === BpmModelFormType.NORMAL) {
			flow.formItems = FcDesigner.formCreate.toJson(formConfig.formRule || []);
		} else {
			flow.formItems = JSON.stringify([]);
			delete formConfig.formRule;
		}

		flow.formConfig = FcDesigner.formCreate.toJson(formConfig);
		flow.process = FcDesigner.formCreate.toJson(processData);
		flow.adminList = FcDesigner.formCreate.toJson(store.step1.adminList);

		await addFlow(flow);
		validateDialogShow.value = false;
		store.$reset();
		await router.push('/flow/list/index');
	} finally {
		isSubmitting.value = false;
	}
};

const getQueryValue = (value: LocationQueryValue | LocationQueryValue[]): string => {
	if (Array.isArray(value)) return value[0] ?? '';
	return value ?? '';
};

onMounted(() => {
	const groupId = getQueryValue(route.query.groupId);
	const flowId = getQueryValue(route.query.flowId);
	const cp = getQueryValue(route.query.cp);
	const { userId, username, avatar } = useUserInfo().userInfos.user;

	store.clearStep1();
	store.clearStep2();
	store.step1.adminList = [{ id: userId, name: username, avatar, type: 'user' }];
	step3NodeConfig.value = {};

	if (groupId) {
		paramGroupId.value = groupId;
	}

	if (flowId) {
		getFlowDetail(flowId).then((res) => {
			const { data } = res;
			store.step1.adminList = JSON.parse(data.adminList || '[]');
			store.step1.name = data.name;
			store.step1.logo = data.logo;
			if (cp !== '1') {
				store.step1.flowId = flowId;
			} else {
				store.step1.flowId = '';
			}
			store.step1.remark = data.remark;
			store.step1.groupId = data.groupId;

			if (data.formConfig) {
				try {
					const formConfig = FcDesigner.formCreate.parseJson(data.formConfig);
					store.step2.formType = formConfig.formType || BpmModelFormType.NORMAL;
					const optionalFields = ['formCustomCreatePath', 'formCustomViewPath', 'formId', 'formRule', 'formOption'] as const;
					for (const field of optionalFields) {
						if (formConfig[field] !== undefined) {
							store.step2[field] = formConfig[field];
						}
					}
				} catch {
					ElMessage.error('加载表单配置失败，请检查流程数据');
				}
			}

			step3NodeConfig.value = JSON.parse(data.process);
		});
		return;
	}
});

watch(activeStep, (newStep, oldStep) => {
	if (oldStep === 1 && newStep !== 1) {
		step2Ref.value?.saveFormDesign();
	}
});
</script>

<template>
	<div class="layout-padding layout-padding-unset flow-create-page">
		<section class="layout-padding-auto layout-padding-view layout-padding-unset layout-padding-unset-view flow-create-shell" aria-label="创建流程">
			<CreateFlowHeader
				:active-step="activeStep"
				:steps="steps"
				:saved-label="savedLabel"
				@back="router.push('/flow/list/index')"
				@navigate="navigateToStep"
			/>

			<main class="flow-create-content">
				<Step1 v-show="activeStep === 0" ref="step1Ref" :group-id="paramGroupId" />
				<Step2 v-show="activeStep === 1" ref="step2Ref" />
				<Step3 v-show="activeStep === 2" ref="step3Ref" :node-config-obj="step3NodeConfig" />
			</main>

			<CreateFlowFooter
				:primary-label="primaryLabel"
				:previous-label="previousLabel"
				@previous="handlePreviousAction"
				@save="saveCurrentStep"
				@primary="handlePrimaryAction"
			/>
		</section>

		<el-dialog
			v-model="validateDialogShow"
			class="flow-validation-dialog"
			:title="$t('flow.processCheck')"
			width="640px"
			:close-on-click-modal="false"
		>
			<el-steps :active="validateFlowStep" finish-status="success" simple>
				<el-step :title="$t('flow.basicInformation')" />
				<el-step :title="$t('flow.formDesign')" />
				<el-step :title="$t('flow.processDesign')" />
			</el-steps>

			<el-result
				v-if="validateFlowStep === 3 && validateErrMsg.length === 0 && !validatingShow"
				icon="success"
				:title="$t('flow.checkSuccess')"
				:sub-title="$t('flow.checkSubSuccess')"
			>
				<template #extra>
					<el-button type="primary" :loading="isSubmitting" @click="submitFlow">{{ $t('flow.submit') }}</el-button>
				</template>
			</el-result>

			<el-result v-else-if="validatingShow" :title="$t('flow.checkIng')" :sub-title="$t('flow.checkSubIng')">
				<template #icon>
					<div v-loading="true" class="flow-validation-dialog__loading" aria-label="正在检查"></div>
				</template>
			</el-result>

			<el-result v-else-if="validateErrMsg.length > 0" icon="error" title="检查失败" sub-title="请修正以下问题后重新发布">
				<template #extra>
					<ul class="flow-validation-dialog__errors">
						<li v-for="item in validateErrMsg" :key="item">
							<el-icon><WarnTriangleFilled /></el-icon>
							<span>{{ item }}</span>
						</li>
					</ul>
					<el-button type="primary" @click="gotoEdit">{{ $t('common.editBtn') }}</el-button>
				</template>
			</el-result>
		</el-dialog>
	</div>
</template>

<style scoped>
.flow-create-page {
	padding: 0 !important;
	background: var(--el-bg-color);
}

.flow-create-shell {
	width: 100%;
	min-width: 0;
	padding: 0 !important;
	border-radius: 0 !important;
	background: var(--el-bg-color);
	box-shadow: none !important;
	display: flex;
	flex-direction: column;
}

.flow-create-shell:hover {
	box-shadow: none !important;
}

.flow-create-content {
	min-height: 0;
	flex: 1;
	overflow: hidden;
	background: var(--el-bg-color);
}

.flow-create-content > * {
	width: 100%;
	height: 100%;
}

.flow-validation-dialog__loading {
	width: 96px;
	height: 96px;
}

.flow-validation-dialog__errors {
	display: grid;
	gap: 8px;
	max-height: 180px;
	margin: 0 0 20px;
	padding: 12px 16px;
	overflow: auto;
	border: 1px solid var(--el-color-danger-light-7);
	border-radius: var(--el-border-radius-base);
	color: var(--el-color-danger);
	background: var(--el-color-danger-light-9);
	list-style: none;
	text-align: left;
}

.flow-validation-dialog__errors li {
	display: flex;
	align-items: flex-start;
	gap: 8px;
}

@media (max-width: 768px) {
	.flow-create-page :deep(.layout-padding-auto) {
		padding: 0;
	}
}
</style>
