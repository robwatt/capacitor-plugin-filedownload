'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@capacitor/core');

const FileDownload = core.registerPlugin('FileDownload', {
    web: () => Promise.resolve().then(function () { return web; }).then(m => new m.FileDownloadWeb()),
});

class FileDownloadWeb extends core.WebPlugin {
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

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    FileDownloadWeb: FileDownloadWeb
});

exports.FileDownload = FileDownload;
//# sourceMappingURL=plugin.cjs.js.map
