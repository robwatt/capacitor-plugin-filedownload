import { WebPlugin } from '@capacitor/core';
export class FileDownloadWeb extends WebPlugin {
    openSetting() {
        throw new Error('Method not implemented onWeb.');
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    async download(_options) {
        return { path: '' };
    }
    async cancel() {
        return;
    }
    async isCanceled() {
        return {
            isCanceled: false
        };
    }
    async checkPermissions() {
        return {
            storage: "prompt"
        };
    }
    async requestPermissions() {
        return {
            storage: "prompt"
        };
    }
}
//# sourceMappingURL=web.js.map