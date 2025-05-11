package io.homo.superresolution.api.registry;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.utils.Requirement;

import java.util.Objects;
import java.util.UUID;

public class AlgorithmDescription<T extends AbstractAlgorithm> {
    public final String briefName;
    public final String codeName;
    public final String displayName;
    public final Requirement requirement;
    protected final Class<T> clazz;
    private final String uuid = UUID.randomUUID().toString();

    public AlgorithmDescription(
            Class<T> clazz,
            String briefName,
            String codeName,
            String displayName,
            Requirement requirement
    ) {
        this.clazz = clazz;
        this.briefName = briefName;
        this.codeName = codeName;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public String getBriefName() {
        return briefName;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUUID() {
        return uuid;
    }

    public T createNewInstance() {
        try {
            T instance = this.clazz.getDeclaredConstructor().newInstance();
            instance.init();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmDescription<?> that = (AlgorithmDescription<?>) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
