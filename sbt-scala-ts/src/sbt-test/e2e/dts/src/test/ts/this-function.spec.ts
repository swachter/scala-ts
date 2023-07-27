import * as m from 'scala-ts-mod'

describe('this function', function() {

    it('listener', function() {
      const n = new m.Notifier
      const l = new m.Listener
      const msg = 'message'
      let x: string = ''
      n.addListener(l.notifyFunction)
      n.addListener((s: string) => x = s)
      n.notify(msg)
      expect(l.s).toBe(msg)
      expect(x).toBe(msg)
    });

});
