import logger from '../logger.js';

describe('logger', () => {
  it('exports logger', () => {
    const infoSpy = jest.spyOn(logger, 'info');
    logger.info('test');
    expect(infoSpy).toHaveBeenCalledWith('test');
    expect(infoSpy).toHaveBeenCalledTimes(1);
  });
});
