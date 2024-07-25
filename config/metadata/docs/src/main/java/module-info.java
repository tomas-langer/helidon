module io.helidon.config.metadata.docs {
    requires io.helidon.metadata.hjson;
    requires handlebars;
    requires org.eclipse.yasson;
    requires jakarta.json.bind;
    requires io.helidon.logging.common;

    exports io.helidon.config.metadata.docs;
}