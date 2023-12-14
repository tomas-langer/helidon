package io.helidon.inject;

class ServicesActivator extends ServiceProviderBase<Services> {
    private ServicesActivator(ServicesImpl services) {
        super(services, Services__ServiceDescriptor.INSTANCE);
    }

    static ServicesActivator create(ServicesImpl services) {
        ServicesActivator response = new ServicesActivator(services);
        response.state(Phase.ACTIVE, services);
        return response;
    }
}
