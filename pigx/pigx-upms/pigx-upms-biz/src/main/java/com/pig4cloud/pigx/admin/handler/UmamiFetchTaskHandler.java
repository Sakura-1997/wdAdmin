/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer.
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
import com.pig4cloud.pigx.admin.service.SysPublicParamService;
import com.pig4cloud.pigx.common.data.tenant.TenantBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Umami API 异步数据拉取任务
 *
 * @author lengleng
 * @date 2026-06-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UmamiFetchTaskHandler {

	private static final String UMAMI_PROVIDER = "umami";

	private static final String HOST_URL_KEY = "SITE_UMAMI_HOST_URL";

	private static final String WEBSITE_ID_KEY = "SITE_UMAMI_WEBSITE_ID";

	private static final int TOP_LIMIT = 5;

	private static final int HTTP_TIMEOUT = 10000;

	private final SysAnalyticsDataMapper analyticsDataMapper;

	private final SysSocialDetailsMapper socialDetailsMapper;

	private final SysPublicParamService sysPublicParamService;

	/**
	 * 异步从 Umami API 拉取数据并存库
	 *
	 * @param tenantId  调用方当前租户 ID
	 * @param numOfDays 统计天数（1=24h, 2=48h, 3=72h）
	 */
	@Async
	public void fetchAndSave(Long tenantId, int numOfDays) {
		try {
			TenantBroker.runAs(tenantId, t -> {
				SysAnalyticsData today = analyticsDataMapper.selectOne(Wrappers.<SysAnalyticsData>lambdaQuery()
					.eq(SysAnalyticsData::getProvider, UMAMI_PROVIDER)
					.eq(SysAnalyticsData::getDataDate, LocalDate.now())
					.eq(SysAnalyticsData::getNumOfDays, numOfDays));
				if (today != null && (AnalyticsFetchStatus.PENDING.equals(today.getFetchStatus())
						|| AnalyticsFetchStatus.SUCCESS.equals(today.getFetchStatus()))) {
					log.debug("[Umami] 今日 numOfDays={} 已有进行中或成功记录，跳过", numOfDays);
					return;
				}

				SysSocialDetails config = socialDetailsMapper
					.selectOne(Wrappers.<SysSocialDetails>lambdaQuery().eq(SysSocialDetails::getType, UMAMI_PROVIDER));
				String hostUrl = normalizeHostUrl(sysPublicParamService.getSysPublicParamKeyToValue(HOST_URL_KEY));
				String websiteId = StrUtil.trim(sysPublicParamService.getSysPublicParamKeyToValue(WEBSITE_ID_KEY));
				if (config == null || StrUtil.hasBlank(hostUrl, websiteId)) {
					log.warn("[Umami] 配置不完整，跳过拉取");
					return;
				}

				Long dataId;
				if (today == null) {
					SysAnalyticsData placeholder = new SysAnalyticsData();
					placeholder.setProvider(UMAMI_PROVIDER);
					placeholder.setDataDate(LocalDate.now());
					placeholder.setNumOfDays(numOfDays);
					placeholder.setFetchStatus(AnalyticsFetchStatus.PENDING);
					analyticsDataMapper.insert(placeholder);
					dataId = placeholder.getId();
					log.info("[Umami] 占位行已插入，id={}，numOfDays={}", dataId, numOfDays);
				}
				else {
					dataId = today.getId();
					updateStatus(dataId, AnalyticsFetchStatus.PENDING);
					log.info("[Umami] 失败记录重新拉取，id={}，numOfDays={}", dataId, numOfDays);
				}

				try {
					String token = login(hostUrl, config);
					if (StrUtil.isBlank(token)) {
						updateStatus(dataId, AnalyticsFetchStatus.FAILED);
						return;
					}

					Map<String, Object> data = fetchWebsiteData(hostUrl, websiteId, token, numOfDays);
					SysAnalyticsData update = new SysAnalyticsData();
					update.setId(dataId);
					update.setFetchStatus(AnalyticsFetchStatus.SUCCESS);
					update.setDataJson(JSONUtil.toJsonStr(data));
					analyticsDataMapper.updateById(update);
					log.info("[Umami] 数据拉取并存库成功，日期：{}，numOfDays：{}", LocalDate.now(), numOfDays);
				}
				catch (Exception e) {
					log.error("[Umami] API 调用失败", e);
					updateStatus(dataId, AnalyticsFetchStatus.FAILED);
				}
			});
		}
		catch (Exception e) {
			log.error("[Umami] 数据拉取异常", e);
		}
	}

	private String normalizeHostUrl(String hostUrl) {
		return StrUtil.removeSuffix(StrUtil.trim(hostUrl), StrUtil.SLASH);
	}

	private String login(String hostUrl, SysSocialDetails config) {
		JSONObject loginBody = JSONUtil.createObj()
			.set("username", config.getAppId())
			.set("password", config.getAppSecret());
		HttpResponse loginResponse = HttpRequest.post(hostUrl + "/api/auth/login")
			.header("Content-Type", "application/json")
			.timeout(HTTP_TIMEOUT)
			.body(loginBody.toString())
			.execute();

		if (!loginResponse.isOk()) {
			log.error("[Umami] 登录失败，状态码：{}", loginResponse.getStatus());
			return StrUtil.EMPTY;
		}
		return JSONUtil.parseObj(loginResponse.body()).getStr("token");
	}

	private Map<String, Object> fetchWebsiteData(String hostUrl, String websiteId, String token, int numOfDays) {
		long endAt = System.currentTimeMillis();
		long startAt = endAt - Duration.ofDays(Math.max(numOfDays, 1)).toMillis();
		String baseQuery = "startAt=%d&endAt=%d".formatted(startAt, endAt);
		String statsUrl = "%s/api/websites/%s/stats?%s".formatted(hostUrl, websiteId, baseQuery);

		HttpResponse statsResponse = authorizedGet(statsUrl, token);
		if (!statsResponse.isOk()) {
			throw new IllegalStateException("Umami stats API 调用失败，状态码：" + statsResponse.getStatus());
		}

		JSONObject stats = JSONUtil.parseObj(statsResponse.body());
		Map<String, Object> data = new HashMap<>();
		Integer pageviews = Convert.toInt(stats.get("pageviews"), 0);
		Integer visits = Convert.toInt(stats.get("visits"), 0);
		Integer visitors = Convert.toInt(stats.get("visitors"), 0);
		Integer bounces = Convert.toInt(stats.get("bounces"), 0);
		Integer totalTime = Convert.toInt(stats.get("totaltime"), 0);

		data.put("pageviews", pageviews);
		data.put("visits", visits);
		data.put("visitors", visitors);
		data.put("bounces", bounces);
		data.put("totalTime", totalTime);
		data.put("pagesPerVisit", divide(pageviews, visits));
		data.put("bounceRate", divide(bounces * 100L, visits));
		data.put("avgVisitDuration", divide(totalTime, visits));
		data.put("deviceData", toJsonRanking(requestMetrics(hostUrl, websiteId, token, baseQuery, "device")));
		data.put("topUrls", toJsonRanking(requestMetrics(hostUrl, websiteId, token, baseQuery, "path")));
		data.put("referrerUrlData", toJsonRanking(requestMetrics(hostUrl, websiteId, token, baseQuery, "referrer")));
		data.put("pageTitleData", toJsonRanking(requestMetrics(hostUrl, websiteId, token, baseQuery, "title")));
		data.put("browserData", toJsonRanking(requestMetrics(hostUrl, websiteId, token, baseQuery, "browser")));
		return data;
	}

	private HttpResponse authorizedGet(String url, String token) {
		return HttpRequest.get(url)
			.header("Authorization", "Bearer " + token)
			.header("Content-Type", "application/json")
			.timeout(HTTP_TIMEOUT)
			.execute();
	}

	private JSONArray requestMetrics(String hostUrl, String websiteId, String token, String baseQuery, String type) {
		String metricsUrl = "%s/api/websites/%s/metrics/expanded?%s&type=%s&limit=%d"
			.formatted(hostUrl, websiteId, baseQuery, type, TOP_LIMIT);
		HttpResponse response = authorizedGet(metricsUrl, token);
		if (!response.isOk()) {
			log.warn("[Umami] metrics type={} 调用失败，状态码：{}", type, response.getStatus());
			return new JSONArray();
		}
		return JSONUtil.parseArray(response.body());
	}

	private BigDecimal divide(long numerator, long denominator) {
		if (denominator == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
	}

	/**
	 * 将 Umami metrics/expanded 转换为前端已有的 [{name,value}] 结构
	 */
	private String toJsonRanking(JSONArray array) {
		List<Map<String, Object>> list = new ArrayList<>();
		array.forEach(item -> {
			if (item instanceof JSONObject obj) {
				list.add(Map.of(
						"name", StrUtil.blankToDefault(obj.getStr("name"), "unknown"),
						"value", Convert.toInt(obj.get("visits"), 0)));
			}
		});
		return JSONUtil.toJsonStr(list);
	}

	private void updateStatus(Long id, AnalyticsFetchStatus status) {
		SysAnalyticsData update = new SysAnalyticsData();
		update.setId(id);
		update.setFetchStatus(status);
		analyticsDataMapper.updateById(update);
	}

}
