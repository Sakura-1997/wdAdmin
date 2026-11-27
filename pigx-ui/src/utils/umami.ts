let initialized = false;

const SCRIPT_ID = 'pigx-umami-script';

const normalizeHostUrl = (hostUrl: string) => hostUrl.trim().replace(/\/+$/, '');

/**
 * 初始化 Umami tracker
 * @param websiteId Umami Website ID
 * @param hostUrl Umami 实例地址
 */
export function initUmami(websiteId: string, hostUrl: string): void {
	if (!websiteId || !hostUrl || initialized) return;

	const normalizedHostUrl = normalizeHostUrl(hostUrl);
	if (!normalizedHostUrl || document.getElementById(SCRIPT_ID)) return;

	const script = document.createElement('script');
	script.id = SCRIPT_ID;
	script.async = true;
	script.defer = true;
	script.src = `${normalizedHostUrl}/script.js`;
	script.dataset.websiteId = websiteId;
	script.dataset.hostUrl = normalizedHostUrl;
	document.head.appendChild(script);
	initialized = true;
}
