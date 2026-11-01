import { createApi } from "@/utils/request";

const request = createApi("/web/feedback");

export const toUp = (data: any) => {
    return request<any>({
        method: "post",
        data,
    });
};
