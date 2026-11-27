/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pigx.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pigx.admin.api.constant.AnalyticsFetchStatus;
import com.pig4cloud.pigx.admin.api.constant.UpmsErrorCodes;
import com.pig4cloud.pigx.admin.api.entity.SysAnalyticsData;
import com.pig4cloud.pigx.admin.handler.ClarityFetchTaskHandler;
import com.pig4cloud.pigx.admin.handler.UmamiFetchTaskHandler;
import com.pig4cloud.pigx.admin.mapper.SysAnalyticsDataMapper;
import com.pig4cloud.pigx.admin.service.SysPublicParamService;
import com.pig4cloud.pigx.admin.service.SysSystemInfoService;
import com.pig4cloud.pigx.common.core.util.MsgUtils;
import com.pig4cloud.pigx.common.core.util.R;
import com.pig4cloud.pigx.common.data.cache.RedisUtils;
import com.pig4cloud.pigx.common.data.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 系统监控服务实现
 *
 * @author lengleng
 * @date 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSystemInfoServiceImpl extends ServiceImpl<SysAnalyticsDataMapper, SysAnalyticsData>
		implements SysSystemInfoService {

	private static final String ANALYTICS_PROVIDER_KEY = "SITE_ANALYTICS_PROVIDER";

	private static final String ANALYTICS_PROVIDER_CLARITY = "clarity";

	private static final String ANALYTICS_PROVIDER_UMAMI = "umami";

	private final ClarityFetchTaskHandler clarityFetchTask;

	private final UmamiFetchTaskHandler umamiFetchTask;

	private final SysPublicParamService sysPublicParamService;

	@Override
	public R<Map<String, Object>> getCacheInfo() {
		Properties info = RedisUtils.execute((RedisCallback<Properties>) RedisServerCommands::info);
		Properties commandStats = RedisUtils
				.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info("commandstats"));
		Object dbSize = RedisUtils.execute((RedisCallback<Object>) RedisServerCommands::dbSize);

		if (commandStats == null) {
			return R.failed(MsgUtils.getMessage(UpmsErrorCodes.SYS_SYSTEM_CACHE_FETCH_ERROR));
		}

		Map<String, Object> result = new HashMap<>(3);
		result.put("info", info);
		result.put("dbSize", dbSize);

		List<Map<String, String>> pieList = new ArrayList<>();
		commandStats.stringPropertyNames().forEach(key -> {
			Map<String, String> data = new HashMap<>(2);
			String property = commandStats.getProperty(key);
			data.put("name", StringUtils.removeStart(key, "cmdstat_"));
			data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
			pieList.add(data);
		});

		result.put("commandStats", pieList);
		return R.ok(result);
	}

	@Override
	public R<?> getClarityData(int numOfDays) {
		return getProviderData(ANALYTICS_PROVIDER_CLARITY, numOfDays);
	}

	@Override
	public R<?> getAnalyticsData(int numOfDays) {
		String provider = StrUtil.trim(StrUtil.blankToDefault(
				sysPublicParamService.getSysPublicParamKeyToValue(ANALYTICS_PROVIDER_KEY), ANALYTICS_PROVIDER_CLARITY))
			.toLowerCase(Locale.ROOT);
		return getProviderData(ANALYTICS_PROVIDER_UMAMI.equals(provider) ? ANALYTICS_PROVIDER_UMAMI
				: ANALYTICS_PROVIDER_CLARITY, numOfDays);
	}

	private R<?> getProviderData(String provider, int numOfDays) {
		SysAnalyticsData today = baseMapper.selectOne(Wrappers.<SysAnalyticsData>lambdaQuery()
			.eq(SysAnalyticsData::getProvider, provider)
			.eq(SysAnalyticsData::getDataDate, LocalDate.now())
			.eq(SysAnalyticsData::getNumOfDays, numOfDays));

		if (today != null) {
			if (AnalyticsFetchStatus.SUCCESS.equals(today.getFetchStatus())) {
				return R.ok(withProvider(today.getDataJson(), provider));
			}
			if (AnalyticsFetchStatus.PENDING.equals(today.getFetchStatus())) {
				return R.ok(loading(provider));
			}
		}

		fetch(provider, numOfDays);
		return R.ok(loading(provider));
	}

	private Map<String, Object> loading(String provider) {
		Map<String, Object> loading = new HashMap<>(3);
		loading.put("loading", true);
		loading.put("message", "数据获取中，请稍后手动刷新");
		loading.put("provider", provider);
		return loading;
	}

	private Map<String, Object> withProvider(String dataJson, String provider) {
		Map<String, Object> result = new HashMap<>();
		if (StrUtil.isNotBlank(dataJson)) {
			JSONUtil.parseObj(dataJson).forEach((key, value) -> result.put(String.valueOf(key), value));
		}
		result.put("provider", provider);
		return result;
	}

	private void fetch(String provider, int numOfDays) {
		Long tenantId = TenantContextHolder.getTenantId();
		if (ANALYTICS_PROVIDER_UMAMI.equals(provider)) {
			umamiFetchTask.fetchAndSave(tenantId, numOfDays);
			return;
		}
		clarityFetchTask.fetchAndSave(tenantId, numOfDays);
	}

}
