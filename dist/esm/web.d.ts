import { WebPlugin } from '@capacitor/core';
import type { CancelStatus, FileDownloadPlugin, FileDownloadOptions, FileDownloadResponse, PermissionStatus } from './definitions';
export declare class FileDownloadWeb extends WebPlugin implements FileDownloadPlugin {
    openSetting(): Promise<void>;
    download(_options?: FileDownloadOptions): Promise<FileDownloadResponse>;
    cancel(): Promise<void>;
    isCanceled(): Promise<CancelStatus>;
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(): Promise<PermissionStatus>;
}
