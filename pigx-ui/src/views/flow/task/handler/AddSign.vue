<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue';
import { ElDialog, ElDrawer } from 'element-plus';
import { addSign } from '/@/api/flow/task';
import type { AddSignResult, FlowUserId } from '/@/types/flow-task';
import OrgSelector from '/@/components/OrgSelector/index.vue';
import type { OrgItem } from '/@/components/OrgSelector/types';
import { useMessage } from '/@/hooks/message';

interface Props {
	modelValue: boolean;
	taskId: string;
	participantUserIds?: FlowUserId[];
	mobile?: boolean;
}

interface Emits {
	'update:modelValue': [value: boolean];
	success: [result: AddSignResult];
}

const props = withDefaults(defineProps<Props>(), {
	participantUserIds: () => [],
	mobile: false,
});

const emit = defineEmits<Emits>();
const selectedUsers = ref<OrgItem[]>([]);
const reason = shallowRef('');
const submitLoading = shallowRef(false);

const visible = computed({
	get: () => props.modelValue,
	set: (value: boolean) => emit('update:modelValue', value),
});

const overlayComponent = computed(() => (props.mobile ? ElDrawer : ElDialog));
const overlayProps = computed(() =>
	props.mobile ? { direction: 'btt' as const, size: '78%', appendToBody: true } : { width: '500px', appendToBody: true }
);

function resetForm() {
	selectedUsers.value = [];
	reason.value = '';
}

function validateForm() {
	if (selectedUsers.value.length === 0) {
		useMessage().warning('请选择加签人员');
		return false;
	}

	const trimmedReason = reason.value.trim();
	if (trimmedReason.length < 2 || trimmedReason.length > 100) {
		useMessage().warning('加签原因需为2-100字');
		return false;
	}

	return true;
}

function getErrorMessage(error: unknown) {
	if (error instanceof Error) return error.message;
	if (error && typeof error === 'object' && 'msg' in error && typeof error.msg === 'string') return error.msg;
	return '加签失败，请重试';
}

async function submit() {
	if (submitLoading.value || !validateForm()) return;
	if (!props.taskId) {
		useMessage().warning('缺少任务标识');
		return;
	}

	submitLoading.value = true;
	try {
		const response = await addSign({
			taskId: props.taskId,
			targetUserIdList: selectedUsers.value.map((user) => user.id),
			reason: reason.value.trim(),
		});
		useMessage().success('加签成功');
		emit('success', response.data);
		visible.value = false;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error));
	} finally {
		submitLoading.value = false;
	}
}

watch(
	() => props.modelValue,
	(isOpen) => {
		if (isOpen) resetForm();
	}
);
</script>

<template>
	<component
		:is="overlayComponent"
		v-model="visible"
		v-bind="overlayProps"
		title="加签"
		destroy-on-close
		:close-on-click-modal="!submitLoading"
		:close-on-press-escape="!submitLoading"
	>
		<el-form class="space-y-5" label-position="top" @submit.prevent="submit">
			<el-form-item label="加签人员" required>
				<OrgSelector v-model="selectedUsers" type="user" :multiple="true" :disabled-user-ids="participantUserIds" :compact="mobile" />
				<p class="mb-0 mt-2 text-sm text-slate-600 dark:text-slate-300">已参与当前会签的人员不可再次选择。</p>
			</el-form-item>

			<el-form-item label="加签原因" required>
				<el-input v-model="reason" type="textarea" :rows="mobile ? 4 : 5" maxlength="100" show-word-limit placeholder="请输入加签原因（2-100字）" />
			</el-form-item>
		</el-form>

		<template #footer>
			<div class="flex justify-end gap-2">
				<el-button :disabled="submitLoading" @click="visible = false">取消</el-button>
				<el-button type="primary" :loading="submitLoading" @click="submit">确认加签</el-button>
			</div>
		</template>
	</component>
</template>
