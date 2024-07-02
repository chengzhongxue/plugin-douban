package la.moony.douban;

import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateHeadProcessor;
import java.util.Properties;

@Component
public class ContactDoubanWidget implements TemplateHeadProcessor {
    private final PropertyPlaceholderHelper
        PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");
    private final PluginContext pluginContext;

    public Mono<Void> process(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        return Mono.just(this.contactFormHtml()).doOnNext((html) -> {
            IModelFactory modelFactory = context.getModelFactory();
            model.add(modelFactory.createText(html));
        }).then();
    }

    private String contactFormHtml() {
        Properties properties = new Properties();
        properties.setProperty("version", pluginContext.getVersion());
        properties.setProperty("pluginStaticPath", "/plugins/plugin-douban/assets/static");
        return this.PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(
            "<link href=\"${pluginStaticPath}/style.css?version=${version}\" rel=\"stylesheet\"/>\n "
            + "<script src=\"${pluginStaticPath}/contact-douban.iife.js?version=${version}\"></script>\n ", properties);
    }

    public ContactDoubanWidget(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }
}
