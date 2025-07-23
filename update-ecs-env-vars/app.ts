// @ts-check
import {
    ECSClient,
    DescribeTaskDefinitionCommand,
    RegisterTaskDefinitionCommand,
    UpdateServiceCommand,
    RegisterTaskDefinitionCommandInput,
    ListServicesCommand,
    DescribeServicesCommand,
    ListServicesCommandOutput,
    ListClustersCommand,
    DescribeClustersCommand
} from "@aws-sdk/client-ecs";
import { SSMClient, GetParametersCommand } from "@aws-sdk/client-ssm";
import { Context, EventBridgeEvent } from "aws-lambda";

export const processEnvironmentVariable = async (environmentVar: any, prefix: any, event: EventBridgeEvent<any, any>): Promise<boolean> => {
    //see if this param has an env var with this event:
    // @ts-ignore
    if (event.detail.name.substring(prefix.ssmParamPrefix.length) == environmentVar.name) {
        console.log("environmentVar matched!!: ");
        console.log(environmentVar.name);
        //now to get the parameter value
        const getParameterInput = {
            Names : [
                event.detail.name
            ]
        }

        const myRegion: string = "eu-west-2";
        const client = new SSMClient({apiVersion: "2014-11-13", region: myRegion});
        const getParameterCommand = new GetParametersCommand(getParameterInput);
        const getParameterResponse = await client.send(getParameterCommand);
        if(getParameterResponse.Parameters) {
            environmentVar.value = getParameterResponse.Parameters[0].Value;
            return true;
        }
    }
    return false;
}

export const processContainerDefinition = async (containerDefn: any, prefix: any, event: EventBridgeEvent<any, any>): Promise<boolean> => {
    console.log("containerDefns: ");
    console.log(containerDefn);
    let updated = false;
    if (containerDefn.environment) {
        for(const environmentVar of containerDefn.environment){
            const varUpdated = await processEnvironmentVariable(environmentVar, prefix, event);
            if (varUpdated) updated = true;
        }
    }
    return updated;
}

export const processServiceDefinition = async (serviceDefn: any, clusterDescription: any, prefix: any, event: EventBridgeEvent<any, any>, ecsClient: ECSClient): Promise<object | null> => {
    console.log("serviceDefn: ");
    console.log(serviceDefn);
    //const containerIndex = servicesDesc.services.indexOf(serviceDefn);
    //get the task definition of the service
    let doUpdate: boolean = false;
    const descParams = {
        taskDefinition: serviceDefn.taskDefinition
    };
    const describeTaskDefinitionCommand = new DescribeTaskDefinitionCommand(descParams);
    let taskDefnDesc = await ecsClient.send(describeTaskDefinitionCommand);
    console.log('taskDefnDesc: ');
    console.log(taskDefnDesc);
    if (taskDefnDesc.taskDefinition && taskDefnDesc.taskDefinition.containerDefinitions) {
        for (const containerDefn of taskDefnDesc.taskDefinition.containerDefinitions) {
            const updated = await processContainerDefinition(containerDefn, prefix, event);
            if (updated) doUpdate = true;
        }
        if (doUpdate) {
            console.log("taskDefnDesc: ");
            console.log(taskDefnDesc);
            //register new definition:
            //taskDefnDesc.taskDefinition.family =
            //const registerTaskDefinitionCommandInput = new RegisterTaskDefinitionCommandInput
            const registerTaskDefinitionCommand = new RegisterTaskDefinitionCommand(<RegisterTaskDefinitionCommandInput>taskDefnDesc.taskDefinition);
            const registerTaskDefnResponse = await ecsClient.send(registerTaskDefinitionCommand);
            if (registerTaskDefnResponse) {
                console.log("registerTaskDefnResponse: ");
                console.log(registerTaskDefnResponse);
                if (registerTaskDefnResponse.taskDefinition) {
                    //apply new definition:

                    const updateParams = {
                        cluster: clusterDescription.clusterArn,
                        service: serviceDefn.serviceName,
                        taskDefinition: registerTaskDefnResponse.taskDefinition.taskDefinitionArn
                    };
                    const updateServiceCommand = new UpdateServiceCommand(updateParams);
                    const updateServiceResponse = await ecsClient.send(updateServiceCommand);

                    if (updateServiceResponse) {
                        console.log("updateServiceResponse: ");
                        console.log(updateServiceResponse);
                        //remove the old task definitions?
                        return {
                            statusCode: 200,
                            headers: {
                                "Content-Type": "text/plain"
                            },
                            body: `testing... updateServiceResponse`
                        }
                    } else {
                        return {
                            statusCode: 200,
                            headers: {
                                "Content-Type": "text/plain"
                            },
                            body: `testing... no updateServiceResponse`
                        }
                    }
                }
            }
        }
    }
    return null;
}

export const processService = async (serviceArn: string, clusterDescription: any, prefix: any, event: EventBridgeEvent<any, any>, ecsClient: ECSClient): Promise<object | null> => {
    console.log("serviceArn: ");
    console.log(serviceArn);
    //get the service:
    const params = {
        cluster: clusterDescription.clusterArn,
        services: [serviceArn]
    };
    const describeServicesCommand = new DescribeServicesCommand(params);
    const servicesDesc = await ecsClient.send(describeServicesCommand);
    if (servicesDesc.services) {
        console.log(servicesDesc.services);
        for (const serviceDefn of servicesDesc.services) {
            const result = await processServiceDefinition(serviceDefn, clusterDescription, prefix, event, ecsClient);
            if (result) return result;
        }
    }
    return null;
}

export const processCluster = async (clusterDescription: any, prefix: any, event: EventBridgeEvent<any, any>, ecsClient: ECSClient): Promise<object | null> => {
    if(clusterDescription.clusterName
        && (clusterDescription.clusterName.substring(0,prefix.clusterNamePrefix.length) == prefix.clusterNamePrefix)) {
        console.log("clusterArn: ");
        console.log(clusterDescription.clusterArn);
        //get the list of services:
        const listServicesParams = {
            cluster: clusterDescription.clusterArn
        };
        const listServicesCommand = new ListServicesCommand(listServicesParams);
        console.log("Pre await");

        const listOfServices: ListServicesCommandOutput = await ecsClient.send(listServicesCommand);
        console.log("listOfServices: ");
        console.log(listOfServices);
        if (listOfServices.serviceArns) {
            console.log("listOfServices: " + listOfServices.serviceArns);
            for (const serviceArn of listOfServices.serviceArns) {
                const result = await processService(serviceArn, clusterDescription, prefix, event, ecsClient);
                if (result) return result;
            }
        }
    }
    return null;
}

export const processPrefix = async (prefix: any, event: EventBridgeEvent<any, any>): Promise<object> => {
    console.log(prefix);
    if (event.detail.name.substring(0, prefix.ssmParamPrefix.length) == prefix.ssmParamPrefix) {
        console.log('Event name matched!');
        console.log(event.detail.name);
        const myRegion: string = "eu-west-2";
        const ecsClient = new ECSClient({apiVersion: "2014-11-13", region: myRegion});
        console.log(ecsClient);
        //get the list of clusters
        const listClustersParams = {};
        const listClustersCommand = new ListClustersCommand(listClustersParams);
        const listClustersResult = await ecsClient.send(listClustersCommand);
        console.log("listClustersResult: ");
        console.log(listClustersResult);

        if (listClustersResult.clusterArns) {
            console.log("clusterArns: ");
            console.log(listClustersResult.clusterArns);
            const describeClustersParams = {
                clusters: listClustersResult.clusterArns
            };
            const describeClustersCommand = new DescribeClustersCommand(describeClustersParams);
            const describeClustersResult = await ecsClient.send(describeClustersCommand);
            if(describeClustersResult.clusters){
                for (const clusterDescription of describeClustersResult.clusters) {
                    const result = await processCluster(clusterDescription, prefix, event, ecsClient);
                    if (result) return result;
                }
            }
        }
    }
    return {
        statusCode: 200,
        headers: {
            "Content-Type": "text/plain"
        },
        body: `testing...last else`
    }
}

export const lambdaHandler = async (
  event: EventBridgeEvent<any, any>,
  context: Context
): Promise<object> => {
    console.log(event);
    let response: any = "";
    //check this is an event for this lambda
    console.log(process.env.PREFIXES);
    if(process.env.PREFIXES) {
        const prefixes = JSON.parse(process.env.PREFIXES);
        console.log(prefixes);
        for(const prefix of prefixes) {
            response = await processPrefix(prefix, event);
        }
    }
    return response;
}

