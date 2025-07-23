import { jest } from '@jest/globals';
import { ECSClient } from '@aws-sdk/client-ecs';
import { SSMClient } from '@aws-sdk/client-ssm';
import {
  processEnvironmentVariable,
  processContainerDefinition,
  processServiceDefinition,
  processService,
  processCluster,
  lambdaHandler
} from './app';

// Mock AWS SDK clients
jest.mock('@aws-sdk/client-ecs');
jest.mock('@aws-sdk/client-ssm');

describe('processEnvironmentVariable', () => {
  const mockSSMSend = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (SSMClient as jest.MockedClass<typeof SSMClient>).mockImplementation(() => ({
      send: mockSSMSend,
    } as any));
  });

  test('should update environment variable when name matches', async () => {
    const environmentVar = { name: 'TEST_VAR', value: 'old_value' };
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    mockSSMSend.mockResolvedValue({
      Parameters: [{ Value: 'new_value' }]
    });

    const result = await processEnvironmentVariable(environmentVar, prefix, event as any);
    expect(result).toBe(true);
    expect(environmentVar.value).toBe('new_value');
  });

  test('should not update when name does not match', async () => {
    const environmentVar = { name: 'OTHER_VAR', value: 'old_value' };
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    const result = await processEnvironmentVariable(environmentVar, prefix, event as any);
    expect(result).toBe(false);
    expect(environmentVar.value).toBe('old_value');
  });
});

describe('processContainerDefinition', () => {
  test('should return true when environment variables are updated', async () => {
    const containerDefn = {
      environment: [
        { name: 'TEST_VAR', value: 'old_value' }
      ]
    };
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    const mockSSMSend = jest.fn().mockResolvedValue({
      Parameters: [{ Value: 'new_value' }]
    });
    (SSMClient as jest.MockedClass<typeof SSMClient>).mockImplementation(() => ({
      send: mockSSMSend,
    } as any));

    const result = await processContainerDefinition(containerDefn, prefix, event as any);
    expect(result).toBe(true);
  });

  test('should return false when no environment variables', async () => {
    const containerDefn = {};
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    const result = await processContainerDefinition(containerDefn, prefix, event as any);
    expect(result).toBe(false);
  });
});

describe('processServiceDefinition', () => {
  const mockECSSend = jest.fn();
  const mockECSClient = { send: mockECSSend } as any;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('should update service when container definitions change', async () => {
    const serviceDefn = { taskDefinition: 'task-def-arn', serviceName: 'test-service' };
    const clusterDescription = { clusterArn: 'cluster-arn' };
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    mockECSSend
      .mockResolvedValueOnce({
        taskDefinition: {
          containerDefinitions: [
            { environment: [{ name: 'TEST_VAR', value: 'old_value' }] }
          ]
        }
      })
      .mockResolvedValueOnce({
        taskDefinition: { taskDefinitionArn: 'new-task-def-arn' }
      })
      .mockResolvedValueOnce({ service: 'updated' });

    const mockSSMSend = jest.fn().mockResolvedValue({
      Parameters: [{ Value: 'new_value' }]
    });
    (SSMClient as jest.MockedClass<typeof SSMClient>).mockImplementation(() => ({
      send: mockSSMSend,
    } as any));

    const result = await processServiceDefinition(serviceDefn, clusterDescription, prefix, event as any, mockECSClient);
    expect(result).toEqual({
      statusCode: 200,
      headers: { "Content-Type": "text/plain" },
      body: "testing... updateServiceResponse"
    });
  });
});

describe('processService', () => {
  const mockECSSend = jest.fn();
  const mockECSClient = { send: mockECSSend } as any;

  test('should process service definitions', async () => {
    const serviceArn = 'service-arn';
    const clusterDescription = { clusterArn: 'cluster-arn' };
    const prefix = { ssmParamPrefix: '/stubs/test/' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    mockECSSend.mockResolvedValue({
      services: [{ taskDefinition: 'task-def-arn', serviceName: 'test-service' }]
    });

    const result = await processService(serviceArn, clusterDescription, prefix, event as any, mockECSClient);
    expect(mockECSSend).toHaveBeenCalledWith(expect.any(Object));
  });
});

describe('processCluster', () => {
  const mockECSSend = jest.fn();
  const mockECSClient = { send: mockECSSend } as any;

  test('should process matching cluster', async () => {
    const clusterDescription = {
      clusterName: 'test-cluster-prefix-123',
      clusterArn: 'cluster-arn'
    };
    const prefix = { clusterNamePrefix: 'test-cluster-prefix-' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    mockECSSend.mockResolvedValue({
      serviceArns: ['service-arn-1']
    });

    const result = await processCluster(clusterDescription, prefix, event as any, mockECSClient);
    expect(mockECSSend).toHaveBeenCalled();
  });

  test('should skip non-matching cluster', async () => {
    const clusterDescription = {
      clusterName: 'other-cluster-123',
      clusterArn: 'cluster-arn'
    };
    const prefix = { clusterNamePrefix: 'test-cluster-prefix-' };
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    const result = await processCluster(clusterDescription, prefix, event as any, mockECSClient);
    expect(result).toBe(null);
  });
});

describe('lambdaHandler', () => {
  const mockContext = {} as any;
  const mockECSSend = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    process.env.PREFIXES = JSON.stringify([
      { clusterNamePrefix: 'test-cluster-', ssmParamPrefix: '/stubs/test/' }
    ]);
    (ECSClient as jest.MockedClass<typeof ECSClient>).mockImplementation(() => ({
      send: mockECSSend,
    } as any));
  });

  test('should process prefixes from environment', async () => {
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    mockECSSend.mockResolvedValue({ clusterArns: [] });

    const result = await lambdaHandler(event as any, mockContext);
    expect(result).toBeDefined();
  });

  test('should return empty response when no prefixes', async () => {
    delete process.env.PREFIXES;
    const event = { detail: { name: '/stubs/test/TEST_VAR' } };

    const result = await lambdaHandler(event as any, mockContext);
    expect(result).toBe("");
  });
});