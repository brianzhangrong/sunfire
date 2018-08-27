package com.ihomefnt.sunfire.admin.config;

import com.ihomefnt.sunfire.admin.utils.SunfireConstant;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@Slf4j
public class InterceptorConfig extends WebMvcConfigurerAdapter {

    @Bean
    public TraceIdInterceptor getInterfaceAuthCheckInterceptor() {
        return new TraceIdInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 多个拦截器组成一个拦截器链
        // addPathPatterns 用于添加拦截规则
        // excludePathPatterns 用户排除拦截
        registry.addInterceptor(getInterfaceAuthCheckInterceptor()).addPathPatterns("/*/**");
        super.addInterceptors(registry);
    }


    /**
     * 微服务间接口访问密钥验证
     *
     * @author xiaochangwei
     */
    class TraceIdInterceptor implements HandlerInterceptor {

        @Override
        public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2,
                Exception arg3) throws Exception {
            MDC.remove(SunfireConstant.TRACE_ID);
        }

        @Override
        public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2,
                ModelAndView arg3) throws Exception {

        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                Object obj) throws Exception {
            String traceId = MDC.get(SunfireConstant.TRACE_ID);
            if (StringUtils.isEmpty(traceId)) {
                traceId = request.getParameter(SunfireConstant.TRACE_ID);
                if (StringUtils.isEmpty(traceId)) {
                    traceId = request.getHeader(SunfireConstant.TRACE_ID);
                    if (StringUtils.isEmpty(traceId)) {
                        traceId = UUID.randomUUID().toString().replaceAll("-", "");
                    }
                }
                log.info("init traceId:{}，{}", traceId, Thread.currentThread().getId());
                MDC.put(SunfireConstant.TRACE_ID, traceId);
            }
            return true;
        }

    }
}
