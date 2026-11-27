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

package com.pig4cloud.pigx.admin.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pigx.admin.api.constant.AnalyticsFetchStatus;
import com.pig4cloud.pigx.admin.api.entity.SysAnalyticsData;
import com.pig4cloud.pigx.admin.api.entity.SysSocialDetails;
import com.pig4cloud.pigx.admin.mapper.SysAnalyticsDataMapper;
import com.pig4cloud.pigx.admin.mapper.SysSocialDetailsMapper;
import com.pig4cloud.pigx.common.data.tenant.TenantBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Clarity API 异步数据拉取任务
 *
 * @author lengleng
 * @date 2026-03-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClarityFetchTaskHandler {

	private static final String CLARITY_API_BASE = "https://www.clarity.ms/export-data/api/v1/project-live-insights?numOfDays=";

	private static final String CLARITY_TYPE = "clarity";

	private static final String CLARITY_PROVIDER = "clarity";

	private static final int HTTP_TIMEOUT = 10000;

	private final SysAnalyticsDataMapper analyticsDataMapper;

	private final SysSocialDetailsMapper socialDetailsMapper;

	/**
	 * 异步从 Clarity API 拉取数据并存库
	 * 策略：先插入占位行（PENDING），调用成功后更新数据（SUCCESS），失败则标记（FAILED）。
	 * 并发时第二个请求查到 PENDING/SUCCESS 即跳过，彻底避免重复调用。
	 * 使用 TenantBroker.runAs 恢复调用线程的租户上下文（@Async 新线程不继承 ThreadLocal）
	 *
	 * @param tenantId  调用方当前租户 ID
	 * @param numOfDays Clarity API numOfDays 参数（1=24h, 2=48h, 3=72h）
	 */
	@Async
	public void fetchAndSave(Long tenantId, int numOfDays) {
		try {
			TenantBroker.runAs(tenantId, t -> {
				// 幂等检查：今日已有 PENDING 或 SUCCESS 记录则跳过（FAILED 允许重试）
				SysAnalyticsData today = analyticsDataMapper.selectOne(Wrappers.<SysAnalyticsData>lambdaQuery()
					.eq(SysAnalyticsData::getProvider, CLARITY_PROVIDER)
					.eq(SysAnalyticsData::getDataDate, LocalDate.now())
					.eq(SysAnalyticsData::getNumOfDays, numOfDays));
				if (today != null && (AnalyticsFetchStatus.PENDING.equals(today.getFetchStatus())
						|| AnalyticsFetchStatus.SUCCESS.equals(today.getFetchStatus()))) {
					log.debug("[Clarity] 今日 numOfDays={} 已有进行中或成功记录，跳过", numOfDays);
					return;
				}

				// 获取 Clarity 配置（type='clarity'）
				SysSocialDetails config = socialDetailsMapper.selectOne(
						Wrappers.<SysSocialDetails>lambdaQuery().eq(SysSocialDetails::getType, CLARITY_TYPE));
				if (config == null) {
					log.warn("[Clarity] 未找到 type='clarity' 的配置，跳过拉取");
					return;
				}

				Long dataId;
				if (today == null) {
					// 插入占位行，防止并发重复调用
					SysAnalyticsData placeholder = new SysAnalyticsData();
					placeholder.setProvider(CLARITY_PROVIDER);
					placeholder.setDataDate(LocalDate.now());
					placeholder.setNumOfDays(numOfDays);
					placeholder.setFetchStatus(AnalyticsFetchStatus.PENDING);
					analyticsDataMapper.insert(placeholder);
					dataId = placeholder.getId();
					log.info("[Clarity] 占位行已插入，id={}，numOfDays={}", dataId, numOfDays);
				}
				else {
					dataId = today.getId();
					updateStatus(dataId, AnalyticsFetchStatus.PENDING);
					log.info("[Clarity] 失败记录重新拉取，id={}，numOfDays={}", dataId, numOfDays);
				}

				// 调用 Clarity API
				HttpResponse response = HttpRequest.get(CLARITY_API_BASE + numOfDays)
					.header("Authorization", "Bearer " + config.getAppSecret())
					.header("Content-Type", "application/json")
					.timeout(HTTP_TIMEOUT)
					.execute();

				if (!response.isOk()) {
					log.error("[Clarity] API 调用失败，状态码：{}", response.getStatus());
					// 标记失败，下次调度可重试
					updateStatus(dataId, AnalyticsFetchStatus.FAILED);
					return;
				}

				// 解析响应并填充数据
				JSONArray body = JSONUtil.parseArray(response.body());
				Map<String, Object> data = parseResponse(body);
				SysAnalyticsData update = new SysAnalyticsData();
				update.setId(dataId);
				update.setFetchStatus(AnalyticsFetchStatus.SUCCESS);
				update.setDataJson(JSONUtil.toJsonStr(data));
				analyticsDataMapper.updateById(update);
				log.info("[Clarity] 数据拉取并存库成功，日期：{}，numOfDays：{}", LocalDate.now(), numOfDays);
			});
		}
		catch (Exception e) {
			log.error("[Clarity] 数据拉取异常", e);
		}
	}

	private void updateStatus(Long id, AnalyticsFetchStatus status) {
		SysAnalyticsData update = new SysAnalyticsData();
		update.setId(id);
		update.setFetchStatus(status);
		analyticsDataMapper.updateById(update);
	}

	/**
	 * 解析 Clarity API 响应体（JSONArray 结构）到实体
	 * 字段映射已通过真实 API 调用确认（2026-03-26）
	 * 所有 count 类字段为 String 类型，使用 Convert.toInt 安全转换（null/空串返回 0）
	 */
	private Map<String, Object> parseResponse(JSONArray responseArray) {
		Map<String, Object> data = new HashMap<>();

		for (Object item : responseArray) {
			if (!(item instanceof JSONObject metric)) {
				continue;
			}
			String metricName = metric.getStr("metricName");
			JSONArray info = metric.getJSONArray("information");
			if (info == null || info.isEmpty()) {
				continue;
			}
			JSONObject first = info.getJSONObject(0);

			switch (metricName) {
				case "Traffic" -> {
					data.put("totalSessions", Convert.toInt(first.getStr("totalSessionCount"), 0));
					data.put("distinctUsers", Convert.toInt(first.getStr("distinctUserCount"), 0));
					data.put("pagesPerSession", first.getBigDecimal("pagesPerSessionPercentage"));
				}
				case "ScrollDepth" -> data.put("scrollDepth", first.getBigDecimal("averageScrollDepth"));
				case "DeadClickCount" -> data.put("deadClickRate", first.getBigDecimal("sessionsWithMetricPercentage"));
				case "RageClickCount" -> data.put("rageClickRate", first.getBigDecimal("sessionsWithMetricPercentage"));
				case "Device" -> data.put("deviceData", toJsonRanking(info.stream(), dev ->
						dev.getStr("name"), dev -> Convert.toInt(dev.getStr("sessionsCount"), 0)));
				case "PopularPages" -> data.put("topUrls", toJsonRanking(info.stream().limit(5), page ->
						page.getStr("url"), page -> Convert.toInt(page.getStr("visitsCount"), 0)));
				case "ReferrerUrl" -> data.put("referrerUrlData", toJsonRanking(info.stream().limit(5), ref ->
						StrUtil.blankToDefault(ref.getStr("url"), ref.getStr("name", "unknown")),
						ref -> Convert.toInt(ref.getStr("sessionsCount"), 0)));
				case "PageTitle" -> data.put("pageTitleData", toJsonRanking(info.stream().limit(5), pt ->
						StrUtil.blankToDefault(pt.getStr("name"), pt.getStr("title", "unknown")),
						pt -> Convert.toInt(pt.getStr("sessionsCount"), 0)));
				case "Browser" -> data.put("browserData", toJsonRanking(info.stream(), br ->
						StrUtil.blankToDefault(br.getStr("name"), "unknown"),
						br -> Convert.toInt(br.getStr("sessionsCount"), 0)));
				default -> {
				}
			}
		}

		return data;
	}

	/**
	 * 将 JSONArray 流转换为 [{name, value}] 格式的 JSON 字符串
	 */
	private String toJsonRanking(Stream<?> stream,
								 Function<JSONObject, String> nameExtractor,
								 Function<JSONObject, Integer> valueExtractor) {
		List<Map<String, Object>> list = new ArrayList<>();
		stream.forEach(item -> {
			if (item instanceof JSONObject obj) {
				list.add(Map.of(
						"name", StrUtil.blankToDefault(nameExtractor.apply(obj), "unknown"),
						"value", valueExtractor.apply(obj)));
			}
		});
		return JSONUtil.toJsonStr(list);
	}

}
