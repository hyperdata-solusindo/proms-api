package com.hyperdata.proms.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = "com.hyperdata.proms",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_repositories = noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule client_contracts_must_be_interfaces = classes()
        .that()
        .resideInAPackage("..client.contract..")
        .should()
        .beInterfaces();

    @ArchTest
    static final ArchRule client_implementations_must_reside_in_impl_and_be_component = classes().that()
        .resideInAPackage("..client.impl..")
        .should()
        .beAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule client_contracts_must_not_return_entities = methods()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..client.contract..")
        .should()
        .notHaveRawReturnType(resideInAPackage("..entity.."));

    @ArchTest
    static final ArchRule controllers_must_be_annotated_with_rest_controller = classes().that()
        .resideInAPackage("..controller..")
        .and()
        .areNotInterfaces()
        .should()
        .beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule controllers_must_implement_api_interface = classes().that()
        .resideInAPackage("com.hyperdata.proms.module..controller..")
        .and()
        .areNotInterfaces()
        .should()
        .implement(resideInAPackage("com.hyperdata.proms.module..controller.."));

    @ArchTest
    static final ArchRule services_must_be_annotated_with_service = classes().that()
        .resideInAPackage("..service..")
        .should()
        .beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule no_fields_should_be_annotated_with_autowired = noFields()
        .should()
        .beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule entities_must_not_be_annotated_with_lombok_data = noClasses()
        .that()
        .resideInAPackage("..entity..")
        .should()
        .beAnnotatedWith("lombok.Data");

    @ArchTest
    static final ArchRule controllers_must_not_be_annotated_with_transactional = noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule controller_methods_must_not_be_annotated_with_transactional = noMethods()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule dtos_must_be_records = classes()
        .that()
        .resideInAPackage("..dto..")
        .and()
        .areNotAnonymousClasses()
        .should()
        .beRecords();

    @ArchTest
    static final ArchRule repository_methods_without_query_must_not_contain_order_by = methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .areNotAnnotatedWith(Query.class)
        .should(new ArchCondition<JavaMethod>(
            "not contain 'OrderBy' in derived query method names (pass Sort parameter or use Level 2 @Query JPQL)") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.getName().contains("OrderBy")) {
                    String message = String.format(
                        "Repository method %s.%s() contains 'OrderBy'. Hardcoding sorting in derived method names is forbidden; pass a Sort parameter or use Level 2 @Query with Ubiquitous Language.",
                        method.getOwner().getSimpleName(),
                        method.getName());
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        });

    @ArchTest
    static final ArchRule repository_methods_without_query_must_not_contain_soft_delete_clauses = methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .areNotAnnotatedWith(Query.class)
        .should(new ArchCondition<JavaMethod>(
            "not contain soft-delete clauses ('DeletedAt') in derived method names (use Level 2 @Query with ubiquitous domain name)") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.getName().contains("DeletedAt")) {
                    String message = String.format(
                        "Repository method %s.%s() contains 'DeletedAt'. Chaining soft-delete conditions in derived method names is forbidden; use Level 2 @Query with a ubiquitous domain name (e.g. findActiveById).",
                        method.getOwner().getSimpleName(),
                        method.getName());
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        });

    @ArchTest
    static final ArchRule repository_methods_without_query_must_not_exceed_two_filter_criteria = methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .areNotAnnotatedWith(Query.class)
        .should(new ArchCondition<JavaMethod>(
            "not have more than 1 'And' clause in derived method names (max 2 filter criteria, use Level 2 @Query JPQL for > 2)") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String name = method.getName();
                int andCount = 0;
                int idx = 0;
                while ((idx = name.indexOf("And", idx)) != -1) {
                    andCount++;
                    idx += 3;
                }
                if (andCount > 1) {
                    String message = String.format(
                        "Repository method %s.%s() has %d 'And' clauses. Level 1 derived methods are strictly limited to max 1-2 criteria (max 1 'And'); use Level 2 @Query for complex queries.",
                        method.getOwner().getSimpleName(),
                        name,
                        andCount);
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        });

    @ArchTest
    static final ArchRule repository_methods_must_not_return_raw_object_arrays = methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .should(new ArchCondition<JavaMethod>(
            "not return raw Object[] or List<Object[]> (use Java 21 Record DTO or Interface Projections)") {

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String returnTypeName = method.getReturnType().getName();
                boolean isObjectArray = returnTypeName.contains("Object[]")
                    || returnTypeName.equals("[Ljava.lang.Object;");

                if (!isObjectArray && method.getReturnType() instanceof JavaParameterizedType pt) {
                    for (JavaType arg : pt.getActualTypeArguments()) {
                        if (arg.getName().contains("Object[]") || arg.getName().equals("[Ljava.lang.Object;")) {
                            isObjectArray = true;
                            break;
                        }
                    }
                }

                if (isObjectArray) {
                    String message = String.format(
                        "Repository method %s.%s() returns raw Object[]. Raw arrays are forbidden; use a Java 21 Record DTO projection with JPQL 'SELECT new com...Dto(...)' or an interface projection.",
                        method.getOwner().getSimpleName(),
                        method.getName());
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        });

    @ArchTest
    static final ArchRule modules_must_not_access_internal_packages_of_other_modules = classes()
        .that()
        .resideInAPackage("com.hyperdata.proms..")
        .should(new ArchCondition<JavaClass>("only access public client contracts and DTOs of other modules") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String callerPackage = javaClass.getPackageName();
                String callerDomain = extractDomainPackage(callerPackage);

                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();
                    String targetDomain = extractDomainPackage(targetPackage);

                    if (targetDomain != null && !targetDomain.equals(callerDomain)) {
                        boolean isContract = targetPackage.startsWith(targetDomain + ".client.contract");
                        boolean isDto = targetPackage.startsWith(targetDomain + ".client.dto");

                        if (!isContract && !isDto) {
                            String message = String.format(
                                "Class %s illegally accesses %s in internal package %s (cross-domain access must go through %s.client.contract or %s.client.dto)",
                                javaClass.getName(),
                                targetClass.getName(),
                                targetPackage,
                                targetDomain,
                                targetDomain);
                            events.add(SimpleConditionEvent.violated(dependency, message));
                        }
                    }
                }
            }
        });

    private static String extractDomainPackage(String packageName) {
        if (!packageName.startsWith("com.hyperdata.proms.module.")) {
            return null;
        }
        String rest = packageName.substring("com.hyperdata.proms.module.".length());
        String[] parts = rest.split("\\.");
        if (parts.length >= 1) {
            return "com.hyperdata.proms.module." + parts[0];
        }
        return null;
    }
}
