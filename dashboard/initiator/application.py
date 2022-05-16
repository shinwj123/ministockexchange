#!/usr/bin/python
# -*- coding: utf8 -*-
"""FIX Application"""
import sys
import quickfix as fix
import time
import logging
from datetime import datetime
import random
__SOH__ = chr(1)

def setup_logger(logger_name, log_file, level=logging.INFO):
    lz = logging.getLogger(logger_name)
    formatter = logging.Formatter('%(asctime)s : %(message)s')
    fileHandler = logging.FileHandler(log_file, mode='w')
    fileHandler.setFormatter(formatter)
    lz.setLevel(level)
    lz.addHandler(fileHandler)
    streamHandler = logging.StreamHandler()
    streamHandler.setFormatter(formatter)
    lz.addHandler(streamHandler) 

setup_logger('logfix', 'logs/message.log')
logfix = logging.getLogger('logfix')


class Application(fix.Application):
    """FIX Application"""
    execID = 0

    def onCreate(self, sessionID):
        self.sessionID = sessionID
        print("onCreate : Session (%s)" % sessionID.toString())
        return

    def onLogon(self, sessionID):
        print("Successful Logon to session '%s'." % sessionID.toString())
        return

    def onLogout(self, sessionID):
        print("Session (%s) logout !" % sessionID.toString())
        return

    def toAdmin(self, message, sessionID):
        msg = message.toString().replace(__SOH__, "|")
        logfix.info("(Admin) S >> %s" % msg)
        return
    def fromAdmin(self, message, sessionID):
        msg = message.toString().replace(__SOH__, "|")
        logfix.info("(Admin) R << %s" % msg)
        return
    def toApp(self, message, sessionID):
        msg = message.toString().replace(__SOH__, "|")
        logfix.info("(App) S >> %s" % msg)
        return
    def fromApp(self, message, sessionID):
        msg = message.toString().replace(__SOH__, "|")
        logfix.info("(App) R << %s" % msg)
        self.onMessage(message, sessionID)
        return

    def onMessage(self, message, sessionID):
        """Processing application message here"""
        pass

    def genExecID(self):
        self.execID += 1
        return str(self.execID)

    def send_random_order(self):
        side = 'BUY' if random.random() > 0.5 else 'SELL'
        self.put_new_order('NVDA', side, 'LIMIT', random.randint(10, 100), round(random.gauss(200, 10)))

    def cancel_order(self, symbol, side, origclord_id, clord_id=None):
        message = fix.Message()
        header = message.getHeader()
        header.setField(fix.MsgType(fix.MsgType_OrderCancelRequest))
        message.setField(fix.OrigClOrdID(origclord_id))
        if clord_id:
            message.setField(fix.ClOrdID(clord_id))
        else:
            message.setField(fix.ClOrdID(self.genExecID()))
            
        message.setField(fix.Symbol(symbol))
        if side == 'BUY':
            message.setField(fix.Side(fix.Side_BUY))
        elif side == 'SELL':
            message.setField(fix.Side(fix.Side_SELL))
        message.setField(fix.TransactTime())
        message.setField(fix.Text("Cancel order {}".format(origclord_id)))
        fix.Session.sendToTarget(message, self.sessionID)

        
    def put_new_order(self, symbol, side, order_type, quantity, price, clord_id=None):
        """Request sample new order single"""
        message = fix.Message()
        header = message.getHeader()

        header.setField(fix.MsgType(fix.MsgType_NewOrderSingle)) #39 = D 
        if clord_id:
            message.setField(fix.ClOrdID(clord_id)) #11 = Unique Sequence Number
        else:
            message.setField(fix.ClOrdID(self.genExecID()))
        
        if side == 'BUY':
            message.setField(fix.Side(fix.Side_BUY)) #43 = 1 BUY
        elif side == 'SELL':
            message.setField(fix.Side(fix.Side_SELL))
        message.setField(fix.Symbol(symbol)) #55 = MSFT
        message.setField(fix.OrderQty(quantity)) #38 = 1000
        if order_type == 'LIMIT':
            message.setField(fix.OrdType(fix.OrdType_LIMIT)) #40=2 Limit Order 
            message.setField(fix.Price(price))
        elif order_type == 'MARKET':
            message.setField(fix.OrdType(fix.OrdType_MARKET))
        else:
            print('unsupported order type: {}'.format(order_type))
        message.setField(fix.HandlInst(fix.HandlInst_AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION))
        message.setField(fix.TimeInForce('0'))
        message.setField(fix.Text("NewOrderSingle"))
        trstime = fix.TransactTime()
        trstime.setString(datetime.utcnow().strftime("%Y%m%d-%H:%M:%S.%f")[:-3])
        message.setField(trstime)

        fix.Session.sendToTarget(message, self.sessionID)

    def run(self):
        """Run"""
        while 1:
            self.send_random_order()
            time.sleep(random.random())
