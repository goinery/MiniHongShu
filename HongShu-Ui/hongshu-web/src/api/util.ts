import request from "@/utils/request";


export const toUp = (data: any) => {
    return request<any>({
        url: "/web/feedback",
        method: "post",
        data,
    });
};